set -euo pipefail

if [[ $# -lt 1 ]]; then
  echo "Kullanım: $0 [-i] <dosya.java>" >&2
  exit 1
fi

inplace=false
if [[ $1 == "-i" ]]; then
  inplace=true
  shift
fi

file=$1

export LC_ALL=C.UTF-8

sed_script=$'
s/[Çç]/c/g;
s/[Ğğ]/g/g;
s/[İIı]/i/g;
s/[Öö]/o/g;
s/[Şş]/s/g;
s/[Üü]/u/g;
'

if $inplace; then
  sed -i.bak "$sed_script" "$file"
else
  sed "$sed_script" "$file"
fi
