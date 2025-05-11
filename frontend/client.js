const TOKEN_KEY = 'jwtToken';
const ROLES_KEY = 'roles';
const USER_KEY  = 'username';

const setToken   = t => localStorage.setItem(TOKEN_KEY, t);
const getToken   = () => localStorage.getItem(TOKEN_KEY);
const removeToken= () => localStorage.removeItem(TOKEN_KEY);

const setRoles   = r => localStorage.setItem(ROLES_KEY, JSON.stringify(r));
const getRoles   = () => JSON.parse(localStorage.getItem(ROLES_KEY) || '[]');
const removeRoles= () => localStorage.removeItem(ROLES_KEY);

const setUser    = u => localStorage.setItem(USER_KEY, u);
const getUser    = () => localStorage.getItem(USER_KEY) || '';
const removeUser = () => localStorage.removeItem(USER_KEY);

const hasRole    = role => getRoles().includes(role);

const qs   = id => document.getElementById(id);
const show = id => { const el = qs(id); if (el) el.style.display = 'block'; };
const hide = id => { const el = qs(id); if (el) el.style.display = 'none'; };
const clear= id => { const el = qs(id); if (el) el.value = ''; };

const STUDENT_ID = qs('studentPortal') ? 'studentPortal' : 'uploadForm';

function showTeacherUI(username) {
  ['loginForm', STUDENT_ID].forEach(hide);
  show('teacherPortal');
  show('logoutBtn');
  qs('welcomeMessage').textContent = `Hoş geldin Öğretmen ${username}!`;
}

function showStudentUI(username) {
  hide('loginForm');
  hide('teacherPortal');
  show(STUDENT_ID);
  show('logoutBtn');
  qs('welcomeMessage').textContent = `Hoş geldin ${username}!`;
}

function resetUI() {
  show('loginForm');
  hide('teacherPortal');
  hide(STUDENT_ID);
  hide('logoutBtn');
  qs('welcomeMessage').textContent = '';
  qs('status').textContent = '';
}

async function login() {
  try {
    const username = qs('username').value;
    const password = qs('password').value;

    const res = await fetch('http://localhost:8080/api/auth/login', {
      method : 'POST',
      headers: { 'Content-Type':'application/json', 'Accept':'application/json' },
      body   : JSON.stringify({ username, password })
    });

    if (!res.ok) throw new Error('Login failed');
    const data = await res.json();       // { token, roles }

    setToken(data.token);
    setRoles(data.roles||[]);
    setUser(username);

    hasRole('ROLE_OGRETMEN') ? showTeacherUI(username) : showStudentUI(username);

  } catch (err) {
    qs('status').textContent = 'Giriş başarısız: ' + err.message;
  }
}

function logout() {
  [removeToken, removeRoles, removeUser].forEach(fn => fn());
  resetUI();
}

async function uploadPdf() {
  try {
    // 1) Token & dosya kontrolü
    const token = getToken();
    if (!token) throw new Error('Token yok, tekrar giriş yap');
    const fileInput = qs('pdfFile');
    const file = fileInput?.files[0];
    if (!file) throw new Error('PDF seçmedin');

    // 2) Ek parametreler
    const username  = getUser();          // localStorage'dan
    const timestamp = Date.now().toString();

    // (Opsiyonel) – HMAC imza
    // ------------------------
    // BACKEND’de signature doğruluyorsan → imza üret
    // Frontend’de sır saklama zor; yine de demo için:
    const SECRET = 'front-demo-secret';   // **gerçekte env/HTTP only çerezden çek**
    const encoder  = new TextEncoder();
    const key      = await crypto.subtle.importKey(
      'raw', encoder.encode(SECRET),
      { name: 'HMAC', hash: 'SHA-256' }, false, ['sign']
    );
    const sigBuf   = await crypto.subtle.sign(
      'HMAC', key, encoder.encode(username + timestamp)
    );
    const signature = [...new Uint8Array(sigBuf)]
                        .map(b => b.toString(16).padStart(2, '0')).join('');

    // 3) FormData
    const formData = new FormData();
    formData.append('file',      file);
    formData.append('username',  username);
    formData.append('timestamp', timestamp);
    formData.append('signature', signature);   // ← opsiyonel

    // 4) İstek
    const res = await fetch('http://localhost:8080/api/pdf/upload', {
      method : 'POST',
      headers: { 'Authorization': `Bearer ${token}` },
      body   : formData
    });

    if (!res.ok) {
      if ([401,403].includes(res.status)) { logout(); throw new Error('Oturum bitti'); }
      throw new Error('Upload failed: ' + res.status);
    }

    qs('status').textContent = await res.text();  // “PDF başarıyla yüklendi”
    clear('pdfFile');
  } catch (err) {
    qs('status').textContent = 'Yükleme başarısız: ' + err.message;
  }
}

async function startAnalysis() {
    try {
      const token = getToken();
      if (!token) throw new Error('Token bulunamadı, lütfen giriş yapın');
  
      const res = await fetch(
        'http://localhost:8080/api/ogretmen/analysis/start',
        {
          method : 'POST',
          headers: { 'Authorization': `Bearer ${token}` }
        }
      );
  
      if (!res.ok) {
        if ([401,403].includes(res.status)) { logout(); throw new Error('Yetkisiz'); }
        throw new Error('Analysis failed: ' + res.status);
      }
  
      qs('status').textContent = await res.text();
    } catch (err) {
      qs('status').textContent = 'Analiz başlatılamadı: ' + err.message;
    }
  }
  

window.addEventListener('DOMContentLoaded', () => {
  const token = getToken();
  if (!token) { resetUI(); return; }

  const username = getUser();
  hasRole('ROLE_OGRETMEN') ? showTeacherUI(username) : showStudentUI(username);
});
