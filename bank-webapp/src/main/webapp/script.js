// script.js - frontend handlers (login, signup, account creation, dashboard)
// All fetch() calls assume same-origin and context path served by your webapp.

document.addEventListener('DOMContentLoaded', () => {
  // Login page
  const loginBtn = document.getElementById('loginBtn');
  if (loginBtn) loginBtn.addEventListener('click', handleLogin);

  // Signup / Account creation
  const form = document.getElementById('accountForm');
  if (form) {
    // gender todo single-select
    const genderList = document.getElementById('genderList');
    genderList && genderList.addEventListener('change', e => {
      if (e.target && e.target.type === 'checkbox') {
        const boxes = genderList.querySelectorAll('input[type="checkbox"]');
        boxes.forEach(b => { if (b !== e.target) b.checked = false; });
      }
    });

    form.addEventListener('submit', handleSignup);
  }

  // Dashboard buttons
  const toastMapper = {
    depositBtn: 'Deposit',
    withdrawBtn: 'Withdrawal',
    balanceBtn: 'Check Balance',
    createSipBtn: 'Create SIP',
    viewSipBtn: 'View SIPs',
    createFdBtn: 'Create FD',
    viewFdBtn: 'View FDs',
    paymentsBtn: 'Make Payment',
    paymentHistoryBtn: 'Payment History',
    profileBtn: 'Profile'
  };
  Object.keys(toastMapper).forEach(id => {
    const el = document.getElementById(id);
    if (el) el.addEventListener('click', () => showToast(`Navigating to: ${toastMapper[id]} (implement navigation)`));
  });
});

async function handleLogin() {
  const email = document.getElementById('loginEmail').value.trim();
  const password = document.getElementById('loginPassword').value;
  const msg = document.getElementById('loginMsg');
  msg.textContent = '';

  if (!email || !password) { msg.textContent = 'Enter email and password.'; return; }

  try {
    const res = await fetch('/bank-webapp/api/auth/login', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify({ email, password })
    });
    const data = await res.json();
    if (!res.ok) { msg.textContent = data.error || 'Login failed'; return; }
    // Store user name and redirect to dashboard
    localStorage.setItem('userId', data.userId);
    localStorage.setItem('firstName', data.firstName || '');
    window.location.href = '/bank-webapp/home.html';
  } catch (e) {
    msg.textContent = 'Network error';
  }
}

function getFormData() {
  const get = id => document.getElementById(id) ? document.getElementById(id).value.trim() : '';
  const genderBox = Array.from(document.querySelectorAll('input[name="gender"]')).find(i => i.checked);
  return {
    firstName: get('firstName'),
    lastName: get('lastName'),
    fatherName: get('fatherName'),
    motherName: get('motherName'),
    gender: genderBox ? genderBox.value : '',
    age: Number(get('age')) || 0,
    phone: get('phone'),
    aadhaar: get('aadhaar'),
    pan: get('pan'),
    email: get('email'),
    password: get('password'),
    accountType: get('accountType')
  };
}

async function handleSignup(ev) {
  ev.preventDefault();
  clearHints();
  const data = getFormData();
  const errs = {};
  if (!data.firstName) errs.firstName = 'Enter name';
  if (!data.lastName) errs.lastName = 'Enter surname';
  if (!data.email) errs.email = 'Enter email';
  if (!data.password) errs.password = 'Enter password';
  if (!data.accountType) errs.accountType = 'Select account type';
  if (!data.gender) errs.gender = 'Select gender';

  if (Object.keys(errs).length > 0) { showErrors(errs); return; }

  try {
    const res = await fetch('/bank-webapp/api/auth/signup', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify(data)
    });
    const out = await res.json();
    if (!res.ok) {
      showToast(out.error || 'Signup failed');
      return;
    }
    showResult(`User created (id ${out.userId}). Now creating account...`);
    // create bank account
    const accRes = await fetch('/bank-webapp/api/accounts/create', {
      method: 'POST',
      headers: {'Content-Type':'application/json'},
      body: JSON.stringify({ userId: out.userId, accountType: data.accountType, initialDeposit: 0 })
    });
    const accData = await accRes.json();
    if (!accRes.ok) {
      showToast(accData.error || 'Account creation failed');
      return;
    }
    showResult(`Account ${accData.accountNumber} created with balance ${accData.balance}. Redirecting to login...`);
    setTimeout(()=> window.location.href = '/bank-webapp/index.html', 1500);
  } catch (e) {
    showToast('Network error during signup');
  }
}

function showErrors(errors) {
  for (const k in errors) {
    const el = document.querySelector(`.hint[data-for="${k}"]`);
    if (el) el.textContent = errors[k];
  }
}

function clearHints() { document.querySelectorAll('.hint').forEach(x=>x.textContent=''); hideResult(); }

function showResult(html) {
  const el = document.getElementById('result');
  if (el) { el.innerHTML = html; el.style.display = 'block'; }
}

let toastTimer = null;
function showToast(text) {
  const t = document.getElementById('toast') || createTempToast();
  t.textContent = text; t.style.display = 'block';
  clearTimeout(toastTimer); toastTimer = setTimeout(()=>t.style.display='none', 3000);
}
function createTempToast() {
  const t = document.createElement('div'); t.id = 'toast'; t.className='toast'; document.body.appendChild(t); return t;
}
function hideResult() { const el = document.getElementById('result'); if (el) { el.style.display='none'; el.innerHTML=''; } }
