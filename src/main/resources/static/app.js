const state = {
  token: localStorage.getItem('accessToken') || '',
  refreshToken: localStorage.getItem('refreshToken') || '',
  email: localStorage.getItem('email') || ''
};

const authCard = document.getElementById('authCard');
const appCard = document.getElementById('appCard');
const authMessage = document.getElementById('authMessage');
const taskMessage = document.getElementById('taskMessage');
const taskList = document.getElementById('taskList');
const currentUser = document.getElementById('currentUser');

const loginTab = document.getElementById('loginTab');
const registerTab = document.getElementById('registerTab');
const loginForm = document.getElementById('loginForm');
const registerForm = document.getElementById('registerForm');

const loginEmail = document.getElementById('loginEmail');
const loginPassword = document.getElementById('loginPassword');

const registerName = document.getElementById('registerName');
const registerEmail = document.getElementById('registerEmail');
const registerPassword = document.getElementById('registerPassword');

const taskForm = document.getElementById('taskForm');
const taskHeadline = document.getElementById('taskHeadline');
const taskDescription = document.getElementById('taskDescription');
const logoutBtn = document.getElementById('logoutBtn');

function setAuthState({ token, refreshToken, email }) {
  state.token = token || '';
  state.refreshToken = refreshToken || '';
  state.email = email || '';
  localStorage.setItem('accessToken', state.token);
  localStorage.setItem('refreshToken', state.refreshToken);
  localStorage.setItem('email', state.email);
}

function clearAuthState() {
  state.token = '';
  state.refreshToken = '';
  state.email = '';
  localStorage.removeItem('accessToken');
  localStorage.removeItem('refreshToken');
  localStorage.removeItem('email');
}

function toggleAuthTab(isLogin) {
  loginTab.classList.toggle('active', isLogin);
  registerTab.classList.toggle('active', !isLogin);
  loginForm.classList.toggle('hidden', !isLogin);
  registerForm.classList.toggle('hidden', isLogin);
  authMessage.textContent = '';
}

function setUiAuthenticated(isAuthenticated) {
  authCard.classList.toggle('hidden', isAuthenticated);
  appCard.classList.toggle('hidden', !isAuthenticated);
  currentUser.textContent = state.email ? `Signed in as ${state.email}` : 'Signed in';
}

async function request(path, options = {}, retry = true) {
  const headers = options.headers || {};
  if (state.token) {
    headers.Authorization = `Bearer ${state.token}`;
  }
  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  const response = await fetch(path, { ...options, headers });

  if (response.status === 401 && retry && state.refreshToken) {
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      return request(path, options, false);
    }
  }

  return response;
}

async function refreshAccessToken() {
  if (!state.refreshToken) return false;

  const response = await fetch('/api/refresh/token', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ refreshToken: state.refreshToken })
  });

  if (!response.ok) {
    clearAuthState();
    setUiAuthenticated(false);
    return false;
  }

  const data = await response.json();
  setAuthState({ token: data.accessToken, refreshToken: data.refreshToken, email: state.email });
  return true;
}

async function login(email, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) throw new Error('Invalid email or password.');

  const data = await response.json();
  setAuthState({ token: data.token, refreshToken: data.refreshToken, email: data.email || email });
  setUiAuthenticated(true);
  await loadTasks();
}

async function register(name, email, password) {
  const response = await fetch('/api/auth/register', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ name, email, password })
  });

  if (!response.ok) {
    throw new Error('Unable to register user.');
  }
}

function renderTasks(tasks) {
  taskList.innerHTML = '';
  tasks.forEach(task => {
    const li = document.createElement('li');
    li.className = 'task-item';
    li.innerHTML = `
      <div>
        <h3>${task.headLine || '(No headline)'}</h3>
        <p>${task.description || ''}</p>
      </div>
      <button class="danger" data-task-id="${task.taskId}" type="button">Delete</button>
    `;
    taskList.appendChild(li);
  });
}

async function loadTasks() {
  const response = await request('/api/tasks');
  if (!response.ok) {
    taskMessage.textContent = 'Could not load tasks.';
    return;
  }
  const tasks = await response.json();
  renderTasks(tasks);
  taskMessage.textContent = '';
}

async function createTask(headLine, description) {
  const response = await request('/api/tasks', {
    method: 'POST',
    body: JSON.stringify({ headLine, description })
  });

  if (!response.ok) throw new Error('Could not create task.');

  await loadTasks();
}

async function deleteTask(taskId) {
  const response = await request(`/api/tasks/${taskId}`, { method: 'DELETE' });
  if (!response.ok) throw new Error('Could not delete task.');
  await loadTasks();
}

loginTab.addEventListener('click', () => toggleAuthTab(true));
registerTab.addEventListener('click', () => toggleAuthTab(false));

loginForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  authMessage.textContent = '';
  try {
    await login(loginEmail.value.trim(), loginPassword.value);
  } catch (err) {
    authMessage.textContent = err.message;
  }
});

registerForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  authMessage.textContent = '';
  try {
    await register(registerName.value.trim(), registerEmail.value.trim(), registerPassword.value);
    authMessage.textContent = 'Registration successful. Please log in.';
    toggleAuthTab(true);
    loginEmail.value = registerEmail.value.trim();
  } catch (err) {
    authMessage.textContent = err.message;
  }
});

taskForm.addEventListener('submit', async (e) => {
  e.preventDefault();
  taskMessage.textContent = '';
  try {
    await createTask(taskHeadline.value.trim(), taskDescription.value.trim());
    taskHeadline.value = '';
    taskDescription.value = '';
  } catch (err) {
    taskMessage.textContent = err.message;
  }
});

taskList.addEventListener('click', async (e) => {
  const button = e.target.closest('button[data-task-id]');
  if (!button) return;

  try {
    await deleteTask(button.getAttribute('data-task-id'));
  } catch (err) {
    taskMessage.textContent = err.message;
  }
});

logoutBtn.addEventListener('click', () => {
  clearAuthState();
  setUiAuthenticated(false);
  taskList.innerHTML = '';
});

(async function init() {
  // Check if redirected from OAuth2 with tokens in URL
  const params = new URLSearchParams(window.location.search);
  const tokenFromUrl = params.get('accessToken');
  const refreshTokenFromUrl = params.get('refreshToken');

  if (tokenFromUrl && refreshTokenFromUrl) {
    // OAuth2 redirect - extract email from token
    try {
      const parts = tokenFromUrl.split('.');
      if (parts.length === 3) {
        const decoded = JSON.parse(atob(parts[1]));
        const email = decoded.sub;
        setAuthState({
          token: tokenFromUrl,
          refreshToken: refreshTokenFromUrl,
          email: email
        });
        // Clean URL
        window.history.replaceState({}, document.title, window.location.pathname);
      }
    } catch (e) {
      console.error('Failed to parse OAuth token', e);
    }
  }

  if (!state.token) {
    setUiAuthenticated(false);
    return;
  }

  setUiAuthenticated(true);
  await loadTasks();
})();
