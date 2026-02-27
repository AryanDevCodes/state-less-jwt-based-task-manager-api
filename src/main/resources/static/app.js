const state = {
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

// Helper function to get cookie value
function getCookie(name) {
  const value = `; ${document.cookie}`;
  const parts = value.split(`; ${name}=`);
  if (parts.length === 2) return parts.pop().split(';').shift();
  return null;
}

function setAuthState({ email }) {
  state.email = email || '';
  localStorage.setItem('email', state.email);
}

function clearAuthState() {
  state.email = '';
  localStorage.removeItem('email');
  // Clear cookies by setting them to expire
  document.cookie = 'accessToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
  document.cookie = 'refreshToken=; Path=/; Expires=Thu, 01 Jan 1970 00:00:01 GMT;';
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
  // No need to manually add Authorization header - cookies are sent automatically
  if (!(options.body instanceof FormData) && !headers['Content-Type']) {
    headers['Content-Type'] = 'application/json';
  }

  // Add credentials to include cookies in requests
  const response = await fetch(path, { ...options, headers, credentials: 'include' });

  if (response.status === 401 && retry) {
    console.log('[Frontend] Got 401, attempting token refresh...');
    const refreshed = await refreshAccessToken();
    if (refreshed) {
      console.log('[Frontend] Token refreshed successfully, retrying request...');
      return request(path, options, false);
    } else {
      console.log('[Frontend] Token refresh failed, logging out...');
    }
  }

  return response;
}

async function refreshAccessToken() {
  const refreshToken = getCookie('refreshToken');
  console.log('[Frontend] Refresh token from cookie:', refreshToken ? 'Found' : 'Not found');
  if (!refreshToken) return false;

  try {
    const response = await fetch('/api/refresh/token', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      credentials: 'include',
      body: JSON.stringify({ refreshToken })
    });

    console.log('[Frontend] Refresh response status:', response.status);
    
    if (!response.ok) {
      clearAuthState();
      setUiAuthenticated(false);
      return false;
    }

    // Tokens are now in cookies, no need to extract from response
    console.log('[Frontend] New tokens set in cookies');
    return true;
  } catch (error) {
    console.error('[Frontend] Refresh error:', error);
    clearAuthState();
    setUiAuthenticated(false);
    return false;
  }
}

async function login(email, password) {
  const response = await fetch('/api/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    credentials: 'include',
    body: JSON.stringify({ email, password })
  });

  if (!response.ok) throw new Error('Invalid email or password.');

  const data = await response.json();
  setAuthState({ email: data.email || email });
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
  // Get pagination/sort controls
  const page = document.getElementById('taskPage')?.value || 0;
  const size = document.getElementById('taskSize')?.value || 10;
  const sortBy = document.getElementById('taskSortBy')?.value || 'taskId';
  const direction = document.getElementById('taskDirection')?.value || 'asc';
  const url = `/api/tasks?page=${page}&size=${size}&sortBy=${sortBy}&direction=${direction}`;
  const response = await request(url);
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

logoutBtn.addEventListener('click', async () => {
  try {
    // Call backend logout endpoint to clear cookies
    await fetch('/api/auth/logout', {
      method: 'POST',
      credentials: 'include'
    });
  } catch (err) {
    console.error('Logout error:', err);
  }
  
  clearAuthState();
  setUiAuthenticated(false);
  taskList.innerHTML = '';
});


(function setupTaskControls() {
  const page = document.getElementById('taskPage');
  const size = document.getElementById('taskSize');
  const sortBy = document.getElementById('taskSortBy');
  const direction = document.getElementById('taskDirection');
  const reloadBtn = document.getElementById('taskReloadBtn');
  if (page && size && sortBy && direction && reloadBtn) {
    page.addEventListener('change', loadTasks);
    size.addEventListener('change', loadTasks);
    sortBy.addEventListener('change', loadTasks);
    direction.addEventListener('change', loadTasks);
    reloadBtn.addEventListener('click', loadTasks);
  }
})();

(async function init() {
  // Check if we have an access token cookie (from OAuth2 or regular login)
  const accessToken = getCookie('accessToken');
  if (accessToken) {
    // Extract email from token
    try {
      const parts = accessToken.split('.');
      if (parts.length === 3) {
        const decoded = JSON.parse(atob(parts[1]));
        const email = decoded.sub;
        setAuthState({ email: email });
      }
    } catch (e) {
      console.error('Failed to parse token', e);
    }
    setUiAuthenticated(true);
    await loadTasks();
  } else {
    setUiAuthenticated(false);
  }
})();
