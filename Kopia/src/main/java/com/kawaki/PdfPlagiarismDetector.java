package com.kawaki;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.commons.text.similarity.JaroWinklerDistance;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class PdfPlagiarismDetector {

    private static final String OPENAI_KEY = "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";
    private static final Path   PDF_DIR    = Paths.get("C:\\Users\\rootz\\Desktop\\kopyaodev\\Kopia\\pdfs");
    private static final Path   OUT_HTML   = Paths.get("C:\\Users\\rootz\\Desktop\\kopyaodev\\Kopia\\pdfs\\rapor.html");
    private static final String MODEL      = "text-embedding-3-small";
    private static final int    BATCH      = 16;

    private static final String ENDPOINT   = "https://api.openai.com/v1/embeddings";
    private static final MediaType JSON    = MediaType.parse("application/json; charset=utf-8");
    private static final ObjectMapper M    = new ObjectMapper();
    private static final OkHttpClient HTTP = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(15))
            .readTimeout(Duration.ofSeconds(60))
            .callTimeout(Duration.ofSeconds(90))
            .build();

    private static final double COS_TH=0.50, JW_TH=0.85;
    private static final JaroWinklerDistance JW = new JaroWinklerDistance();

    public static void main(String[] args) throws Exception {
        System.out.println("PDF klasörü : "+PDF_DIR);
        System.out.println("Rapor       : "+OUT_HTML);

        Map<String,List<String>> docSents = new LinkedHashMap<>();
        try (Stream<Path> files = Files.list(PDF_DIR)) {
            files.filter(p->p.toString().toLowerCase().endsWith(".pdf"))
                 .sorted()
                 .forEach(p -> {
                     try {
                         List<String> s = splitSentences(extractText(p.toFile()));
                         docSents.put(p.getFileName().toString(), s);
                     } catch (IOException e) {
                         System.err.println("PDF okuma hatası ("+p+"): "+e.getMessage());
                     }
                 });
        }
        if (docSents.isEmpty()) { System.err.println("PDF yok."); return; }

        /* Embedding */
        Map<String,List<double[]>> docEmb = new HashMap<>();
        for (var e: docSents.entrySet())
            docEmb.put(e.getKey(), embedOpenAI(e.getValue()));

        /* Karşılaştır */
        List<Group> groups = compare(docSents, docEmb);

        /* Rapor */
        Files.writeString(OUT_HTML, buildHtml(groups, docSents));
        System.out.println("✔ Rapor hazır → "+OUT_HTML);
        HTTP.connectionPool().evictAll();
        Objects.requireNonNull(HTTP.dispatcher().executorService()).shutdown();
    }

    private static String extractText(File f) throws IOException {
        try (PDDocument d = PDDocument.load(f)) {
            return new PDFTextStripper().getText(d).replaceAll("\\s+"," ").trim();
        }
    }
    private static List<String> splitSentences(String t){
        return Arrays.stream(t.split("(?<=[.!?])\\s+"))
                     .map(String::trim).filter(s->s.length()>20).toList();
    }

    private static List<double[]> embedOpenAI(List<String> sents) throws IOException {
        List<double[]> out = new ArrayList<>(sents.size());
        for (int i=0;i<sents.size();i+=BATCH) {
            int to = Math.min(i+BATCH, sents.size());
            out.addAll(callOpenAI(sents.subList(i, to)));
        }
        return out;
    }
    private static List<double[]> callOpenAI(List<String> chunk) throws IOException {
        Map<String,Object> payload = Map.of(
            "model", MODEL,
            "input", chunk
        );
        Request req = new Request.Builder().url(ENDPOINT)
            .addHeader("Authorization","Bearer "+OPENAI_KEY)
            .post(RequestBody.create(M.writeValueAsBytes(payload), JSON))
            .build();

        try (Response res = HTTP.newCall(req).execute()) {
            if (!res.isSuccessful())
                throw new IOException(res.code()+" – "+res.body().string());
            JsonNode root = M.readTree(res.body().string());
            return StreamSupport.stream(root.get("data").spliterator(), false)
                    .map(node -> M.convertValue(node.get("embedding"),
                                 new TypeReference<List<Double>>(){}))
                    .map(list -> list.stream().mapToDouble(d->d).toArray())
                    .toList();
        }
    }

    private static List<Group> compare(Map<String,List<String>> docS,
                                       Map<String,List<double[]>> docE){
        List<Group> gr=new ArrayList<>();
        List<String> names = new ArrayList<>(docS.keySet());
        for(int i=0;i<names.size();i++)
            for(int j=i+1;j<names.size();j++){
                String A=names.get(i), B=names.get(j);
                var sa=docS.get(A); var sb=docS.get(B);
                var ea=docE.get(A); var eb=docE.get(B);
                List<Match> ms=new ArrayList<>();
                double mC=0,mJ=0;
                for(int x=0;x<sa.size();x++)
                    for(int y=0;y<sb.size();y++){
                        double cos=cos(ea.get(x),eb.get(y));
                        double jw =JW.apply(sa.get(x),sb.get(y));
                        if(cos>=COS_TH||jw>=JW_TH){
                            ms.add(new Match(x,y,cos,jw));
                            mC=Math.max(mC,cos); mJ=Math.max(mJ,jw);
                        }
                    }
                if(!ms.isEmpty()) gr.add(new Group(A,B,ms,mC,mJ));
            }
        return gr;
    }
    private static double cos(double[] a,double[] b){
        double d=0,na=0,nb=0;
        for(int i=0;i<a.length;i++){ d+=a[i]*b[i]; na+=a[i]*a[i]; nb+=b[i]*b[i]; }
        return d/(Math.sqrt(na)*Math.sqrt(nb)+1e-8);
    }

private static String buildHtml(List<Group> rows,
                                Map<String,List<String>> txt){
    StringBuilder h = new StringBuilder();
    h.append("""
<!doctype html>
<html lang="tr">
  <head>
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width,initial-scale=1">
    <title>İntihal Raporu</title>
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css"
          rel="stylesheet">
    <style>
      body { padding: 2rem; font-family: system-ui, sans-serif; }
      mark { background:#ffe08c; padding:0 .25rem; border-radius:.25rem; }
      .sent-block { max-height:60vh; overflow-y:auto; line-height:1.5; font-size:.95rem; }
      .sent-block::-webkit-scrollbar{width:.6rem}
      .sent-block::-webkit-scrollbar-thumb{background:#ced4da;border-radius:.25rem}
    </style>
  </head>
  <body>
    <div class="container-xxl">
      <h1 class="mb-4">İntihal Raporu</h1>

      <!-- ÖZET TABLOSU -->
      <h2>Özet</h2>
      <div class="table-responsive">
        <table class="table table-bordered table-hover align-middle">
          <thead class="table-light">
            <tr>
              <th>#</th><th>Belge A</th><th>Belge B</th>
              <th>Eşleşme</th><th>Max Cos</th><th>Max JW</th>
            </tr>
          </thead>
          <tbody>
""");

    int no = 1;
    for (var g : rows) {
        h.append("<tr><td>").append(no++).append("</td><td>")
         .append(g.a).append("</td><td>")
         .append(g.b).append("</td><td><span class=\"badge bg-primary\">")
         .append(g.ms.size()).append("</span></td><td>")
         .append(String.format("%.2f", g.mC)).append("</td><td>")
         .append(String.format("%.2f", g.mJ)).append("</td></tr>");
    }

    h.append("""
          </tbody>
        </table>
      </div>

      <!-- DETAY -->
      <hr class="my-4"/>
      <h2>Detay</h2>
""");

    for (var g : rows) {
        h.append("<h3 class=\"mt-4\">").append(g.a)
         .append(" <span class=\"text-muted\">vs</span> ")
         .append(g.b).append("</h3><div class=\"row g-2\">")
         .append(render(txt.get(g.a),
                        g.ms.stream().map(m -> m.i).collect(Collectors.toSet())))
         .append(render(txt.get(g.b),
                        g.ms.stream().map(m -> m.j).collect(Collectors.toSet())))
         .append("</div>");
    }

    h.append("""
    </div>
  </body>
</html>""");
    return h.toString();
}

private static String render(List<String> sents, Set<Integer> hl){
    StringBuilder sb = new StringBuilder(
        "<div class=\"col-12 col-lg-6\"><div class=\"sent-block p-3 border rounded\">");
    for (int i = 0; i < sents.size(); i++) {
        if (hl.contains(i)) sb.append("<mark>");
        sb.append(sents.get(i));
        if (hl.contains(i)) sb.append("</mark>");
        sb.append(" ");
    }
    return sb.append("</div></div>").toString();
}


    
    private record Match(int i,int j,double c,double jv){}
    private record Group(String a,String b,List<Match> ms,double mC,double mJ){}
}
