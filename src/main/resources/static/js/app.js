// PowerFitnessKurnool - Gym Management & Fitness Platform
// High Performance Client Application Logic

const STATE = {
  token: localStorage.getItem('pfk_token') || null,
  user: JSON.parse(localStorage.getItem('pfk_user') || 'null'),
  activeView: 'dashboard',
  currentRole: 'ADMIN',
  loginTab: 'ADMIN',
  members: [],
  renewalsFilter: 'All',
  foodCategoryFilter: 'All',
  andhraOnly: false,
  activeGoal: 'Lean Bulking',
  selectedRenewMemberId: null,
  charts: {}
};

// ================= INITIALIZATION =================
document.addEventListener('DOMContentLoaded', () => {
  initDateInputs();
  if (STATE.token) {
    verifySession();
  } else {
    showAuthView();
  }
});

function initDateInputs() {
  const today = new Date().toISOString().split('T')[0];
  const admDate = document.getElementById('admissionDate');
  if (admDate) admDate.value = today;
  const trackerDate = document.getElementById('trackerDateInput');
  if (trackerDate) trackerDate.value = today;
}

// ================= AUTHENTICATION & NAVIGATION =================
function showLoginError(msg) {
  const errBox = document.getElementById('loginErrorMsg');
  const errText = document.getElementById('loginErrorText');
  if (errBox && errText) {
    errText.textContent = msg || 'Invalid username or password.';
    errBox.classList.remove('hidden');
  }
  showToast(msg || 'Invalid username or password', 'error');
}

function clearLoginError() {
  const errBox = document.getElementById('loginErrorMsg');
  if (errBox) {
    errBox.classList.add('hidden');
  }
}

function setLoginTab(role) {
  STATE.loginTab = role;
  clearLoginError();
  const tabAdmin = document.getElementById('tabAdmin');
  const tabUser = document.getElementById('tabUser');
  const userLabel = document.getElementById('usernameLabel');
  const userInput = document.getElementById('loginUsername');

  if (role === 'ADMIN') {
    tabAdmin.className = 'flex-1 py-2 text-xs font-bold uppercase tracking-wider rounded-lg transition-all duration-200 bg-gym-orange text-white shadow';
    tabUser.className = 'flex-1 py-2 text-xs font-bold uppercase tracking-wider rounded-lg transition-all duration-200 text-slate-400 hover:text-white';
    if (userLabel) userLabel.textContent = 'Admin Username';
    if (userInput) userInput.placeholder = 'e.g. admin';
  } else {
    tabUser.className = 'flex-1 py-2 text-xs font-bold uppercase tracking-wider rounded-lg transition-all duration-200 bg-gym-orange text-white shadow';
    tabAdmin.className = 'flex-1 py-2 text-xs font-bold uppercase tracking-wider rounded-lg transition-all duration-200 text-slate-400 hover:text-white';
    if (userLabel) userLabel.textContent = 'Member Phone Number';
    if (userInput) userInput.placeholder = 'e.g. 9876543210';
  }
}

function fillDemo(username, password, role) {
  setLoginTab(role);
  const uInput = document.getElementById('loginUsername');
  const pInput = document.getElementById('loginPassword');
  if (uInput) uInput.value = username;
  if (pInput) pInput.value = password;
  clearLoginError();
  handleLogin();
}

async function handleLogin(e) {
  if (e && e.preventDefault) {
    e.preventDefault();
  }

  clearLoginError();
  const usernameInput = document.getElementById('loginUsername');
  const passwordInput = document.getElementById('loginPassword');
  const submitBtn = document.getElementById('loginSubmitBtn');

  const username = usernameInput ? usernameInput.value.trim() : '';
  const password = passwordInput ? passwordInput.value : '';

  // Validation
  if (!username) {
    showLoginError('Please enter your username or phone number.');
    if (usernameInput) usernameInput.focus();
    return false;
  }

  if (!password) {
    showLoginError('Please enter your password.');
    if (passwordInput) passwordInput.focus();
    return false;
  }

  if (submitBtn) {
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<span>Verifying...</span>';
  }

  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username, password })
    });

    const data = await res.json();
    if (!res.ok) {
      showLoginError(data.error || 'Invalid credentials. Please verify username and password.');
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.innerHTML = '<span>Enter PowerFitness</span><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>';
      }
      return false;
    }

    // Save session
    STATE.token = data.token;
    STATE.user = data;
    localStorage.setItem('pfk_token', data.token);
    localStorage.setItem('pfk_user', JSON.stringify(data));

    showToast(`Welcome, ${data.fullName}!`, 'success');
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<span>Enter PowerFitness</span><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>';
    }

    // Transition smoothly to dashboard
    setupAppForUser();
    return false;
  } catch (err) {
    console.error('Login error:', err);
    showLoginError('Unable to connect to server. Please try again.');
    if (submitBtn) {
      submitBtn.disabled = false;
      submitBtn.innerHTML = '<span>Enter PowerFitness</span><svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M14 5l7 7m0 0l-7 7m7-7H3"/></svg>';
    }
    return false;
  }
}

async function verifySession() {
  try {
    const res = await fetch('/api/auth/me', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) {
      logout(true);
      return;
    }
    const data = await res.json();
    if (!data || !data.userId) {
      logout(true);
      return;
    }
    STATE.user = { ...STATE.user, ...data };
    setupAppForUser();
  } catch (e) {
    logout(true);
  }
}

function logout(silent = false) {
  if (STATE.token) {
    fetch('/api/auth/logout', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    }).catch(() => {});
  }
  STATE.token = null;
  STATE.user = null;
  localStorage.removeItem('pfk_token');
  localStorage.removeItem('pfk_user');
  showAuthView();
  if (!silent) {
    showToast('Logged out successfully', 'info');
  }
}

function showAuthView() {
  const authView = document.getElementById('authView');
  const mainApp = document.getElementById('mainApp');
  if (authView) {
    authView.classList.remove('hidden');
    authView.style.display = 'flex';
  }
  if (mainApp) {
    mainApp.classList.add('hidden');
    mainApp.style.display = 'none';
  }
  clearLoginError();
}

function setupAppForUser() {
  const authView = document.getElementById('authView');
  const mainApp = document.getElementById('mainApp');
  if (authView) {
    authView.classList.add('hidden');
    authView.style.display = 'none';
  }
  if (mainApp) {
    mainApp.classList.remove('hidden');
    mainApp.style.display = 'flex';
  }

  const u = STATE.user;
  if (!u) return;

  const nameEl = document.getElementById('sidebarUserName');
  if (nameEl) nameEl.textContent = u.fullName || u.username;
  const roleEl = document.getElementById('sidebarUserRole');
  if (roleEl) roleEl.textContent = u.role === 'ADMIN' ? 'Gym Owner / Admin' : 'Registered Member';
  const photoEl = document.getElementById('sidebarUserPhoto');
  if (photoEl) {
    photoEl.src = u.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100';
  }

  // Render Sidebar Links based on Role
  renderSidebarLinks(u.role);

  // Admin Quick Action Button in Header
  const quickActions = document.getElementById('adminQuickActions');
  if (quickActions) {
    if (u.role === 'ADMIN') {
      quickActions.classList.remove('hidden');
    } else {
      quickActions.classList.add('hidden');
    }
  }

  // Admin Account Button in Header
  const adminAccountBtn = document.getElementById('adminAccountBtn');
  const headerAdminName = document.getElementById('headerAdminName');
  if (adminAccountBtn) {
    if (u.role === 'ADMIN') {
      adminAccountBtn.classList.remove('hidden');
      adminAccountBtn.classList.add('flex');
      if (headerAdminName) headerAdminName.textContent = u.username || 'Admin';
    } else {
      adminAccountBtn.classList.add('hidden');
      adminAccountBtn.classList.remove('flex');
    }
  }

  // Navigate to appropriate role dashboard
  if (u.role === 'ADMIN') {
    navigateTo('dashboard');
  } else {
    navigateTo('userDashboard');
  }

  try {
    loadNotifications();
  } catch (e) {}
}

// ================= SIDEBAR NAVIGATION =================
function renderSidebarLinks(role) {
  const container = document.getElementById('navLinksContainer');
  if (!container) return;
  let links = [];

  if (role === 'ADMIN') {
    links = [
      { id: 'dashboard', label: 'Dashboard', icon: '📊' },
      { id: 'members', label: 'Members', icon: '👥' },
      { id: 'admissions', label: 'New Admission', icon: '➕' },
      { id: 'renewals', label: 'Renewals', icon: '🔄' },
      { id: 'payments', label: 'Payments', icon: '💳' },
      { id: 'supplements', label: 'Supplements', icon: '💊' },
      { id: 'supplementOrders', label: 'Supplement Orders', icon: '📦' },
      { id: 'collectionHistory', label: 'Collection History', icon: '📋' },
      { id: 'upi', label: 'UPI / QR Code', icon: '📱' },
      { id: 'reports', label: 'Reports', icon: '📈' }
    ];
  } else {
    links = [
      { id: 'userDashboard', label: 'My Dashboard', icon: '🏠' },
      { id: 'userProfile', label: 'My Profile', icon: '👤' },
      { id: 'calories', label: 'Calories & Nutrition', icon: '🥗' },
      { id: 'supplements', label: 'Supplements Store', icon: '💊' },
      { id: 'userOrders', label: 'My Supplement Orders', icon: '📦' },
      { id: 'userCollectionHistory', label: 'Collection History', icon: '📋' },
      { id: 'userPayments', label: 'My Payments & UPI', icon: '💳' }
    ];
  }

  container.innerHTML = links.map(link => `
    <button onclick="navigateTo('${link.id}')" id="nav-${link.id}" class="nav-item w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-xs font-bold uppercase tracking-wider text-slate-400 hover:text-white hover:bg-slate-800/80 transition">
      <span class="text-base">${link.icon}</span>
      <span>${link.label}</span>
    </button>
  `).join('');
}

function navigateTo(viewId) {
  if (viewId !== 'admissions') {
    try { stopMemberCamera(); } catch (e) {}
  }
  if (viewId !== 'collectionHistory' && viewId !== 'supplementOrders') {
    try { stopCollectionCamera(); } catch (e) {}
  }
  STATE.activeView = viewId;

  // Header quick actions button: strictly role and view based
  const quickActions = document.getElementById('adminQuickActions');
  if (quickActions) {
    if (STATE.user && STATE.user.role === 'ADMIN') {
      if (viewId === 'supplements') {
        quickActions.innerHTML = `
          <button onclick="openSupplementModal()" class="bg-gym-orange hover:bg-gym-orangeHover text-white text-xs font-bold uppercase px-3.5 py-2 rounded-xl flex items-center gap-1.5 shadow-md shadow-gym-orangeGlow transition">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            <span>+ Add Supplement</span>
          </button>
        `;
        quickActions.classList.remove('hidden');
      } else if (viewId === 'members' || viewId === 'dashboard') {
        quickActions.innerHTML = `
          <button onclick="navigateTo('admissions')" class="bg-gym-orange hover:bg-gym-orangeHover text-white text-xs font-bold uppercase px-3.5 py-2 rounded-xl flex items-center gap-1.5 shadow-md shadow-gym-orangeGlow transition">
            <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 4v16m8-8H4"/></svg>
            <span>+ Add Member</span>
          </button>
        `;
        quickActions.classList.remove('hidden');
      } else {
        quickActions.classList.add('hidden');
      }
    } else {
      quickActions.classList.add('hidden');
    }
  }

  // Update nav buttons
  document.querySelectorAll('.nav-item').forEach(b => {
    b.classList.remove('bg-gym-orange', 'text-white', 'shadow-md', 'shadow-gym-orangeGlow');
    b.classList.add('text-slate-400');
  });
  const activeBtn = document.getElementById(`nav-${viewId}`);
  if (activeBtn) {
    activeBtn.classList.add('bg-gym-orange', 'text-white', 'shadow-md', 'shadow-gym-orangeGlow');
    activeBtn.classList.remove('text-slate-400');
  }

  // Hide all views
  const views = [
    'viewAdminDashboard', 'viewAdmissions', 'viewMembers', 'viewRenewals',
    'viewPayments', 'viewSupplements', 'viewSupplementOrders', 'viewCollectionHistory',
    'viewUpi', 'viewReports',
    'viewUserDashboard', 'viewUserProfile', 'viewCalories', 'viewUserOrders',
    'viewUserCollectionHistory', 'viewUserPayments'
  ];
  views.forEach(v => {
    const el = document.getElementById(v);
    if (el) {
      el.classList.add('hidden');
      el.style.display = 'none';
    }
  });

  // Page header titles
  const titles = {
    dashboard: ['Admin Dashboard', 'Live Gym Operations & Financial Performance'],
    admissions: ['Member Admission', 'Enroll New Athletes with Photo, Plan & Cardio'],
    members: ['Gym Members', 'Search, Filter, View Profiles & Manage Athletes'],
    renewals: ['Renewal Management', 'Track Expiry Durations & Extend Subscriptions'],
    payments: ['Payment Transactions', 'Revenue History & Receipt Generation'],
    supplements: ['Gym Supplements Store', 'Whey, Creatine & Sports Nutrition Desk'],
    supplementOrders: ['Supplement Orders', 'Track Online Purchases, Verify Payments & Hand Over Products'],
    collectionHistory: ['Front Desk Collection History', 'Physical Hand-over Records with Verified Member Camera Snapshots'],
    upi: ['UPI Payment Engine', 'Scan & Pay Configuration with QR Code'],
    reports: ['Analytics & Reports', 'Exportable Financial & Demographic Intelligence'],
    userDashboard: ['Member Dashboard', 'Your Personal Fitness & Membership Headquarters'],
    userProfile: ['Membership ID Card', 'Official PowerFitnessKurnool Athlete Badge'],
    calories: ['Calories & Andhra Diet', 'Goal-Oriented Macro & Nutrition Tracker'],
    userOrders: ['My Supplement Orders', 'Track Online Purchases & Front Desk Collection Status'],
    userCollectionHistory: ['My Collection History', 'Hand-over Receipts with Physical Collection Photos'],
    userPayments: ['My Payments & Fees', 'Scan QR Code & Submit Payment UTR']
  };

  const titlePair = titles[viewId] || ['PowerFitness', 'Kurnool'];
  const pTitle = document.getElementById('pageTitle');
  const pSub = document.getElementById('pageSubtitle');
  if (pTitle) pTitle.textContent = titlePair[0];
  if (pSub) pSub.textContent = titlePair[1];

  // Show selected view
  const targetMap = {
    dashboard: 'viewAdminDashboard',
    admissions: 'viewAdmissions',
    members: 'viewMembers',
    renewals: 'viewRenewals',
    payments: 'viewPayments',
    supplements: 'viewSupplements',
    supplementOrders: 'viewSupplementOrders',
    collectionHistory: 'viewCollectionHistory',
    upi: 'viewUpi',
    reports: 'viewReports',
    userDashboard: 'viewUserDashboard',
    userProfile: 'viewUserProfile',
    calories: 'viewCalories',
    userOrders: 'viewUserOrders',
    userCollectionHistory: 'viewUserCollectionHistory',
    userPayments: 'viewUserPayments'
  };

  const targetEl = document.getElementById(targetMap[viewId]);
  if (targetEl) {
    targetEl.classList.remove('hidden');
    targetEl.style.display = 'block';
  }

  // Trigger data load
  try {
    switch (viewId) {
      case 'dashboard': loadAdminDashboard(); break;
      case 'admissions': recalculateAdmissionFee(); break;
      case 'members': loadMembers(); break;
      case 'renewals': loadRenewals(); break;
      case 'payments': loadPayments(); loadDailyCollection(); break;
      case 'supplements': loadSupplements(); break;
      case 'supplementOrders': loadSupplementOrders(); break;
      case 'collectionHistory': loadAdminCollectionHistory(); break;
      case 'upi': loadUpiSection(); break;
      case 'reports': loadReports(); break;
      case 'userDashboard': loadUserDashboard(); break;
      case 'userProfile': loadUserProfileCard(); break;
      case 'calories': loadNutritionGoals(); loadDailyMealLog(); loadFoodDatabase(); break;
      case 'userOrders': loadUserOrders(); break;
      case 'userCollectionHistory': loadUserCollectionHistory(); break;
      case 'userPayments': loadUserPayments(); break;
    }
  } catch (e) {
    console.error('Error loading view data for ' + viewId, e);
  }
}

// ================= ADMIN DASHBOARD =================
async function loadAdminDashboard() {
  try {
    const res = await fetch('/api/dashboard/admin', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const d = await res.json();

    document.getElementById('kpiActiveMembers').textContent = d.totalActiveMembers ?? 0;
    document.getElementById('kpiExpiredMembers').textContent = d.totalExpiredMembers ?? 0;
    document.getElementById('kpiExpiringSoon').textContent = d.membersExpiringSoon ?? 0;
    document.getElementById('kpiTodayAdmissions').textContent = d.todayAdmissions ?? 0;

    document.getElementById('kpiMorningBatch').textContent = d.morningBatchMembers ?? 0;
    document.getElementById('kpiEveningBatch').textContent = d.eveningBatchMembers ?? 0;
    document.getElementById('kpiCardioMembers').textContent = d.cardioMembers ?? 0;
    document.getElementById('kpiStrengthMembers').textContent = d.strengthMembers ?? 0;

    document.getElementById('kpiTotalRevenue').textContent = formatInr(d.totalMonthlyRevenue);
    document.getElementById('kpiSubRevenue').textContent = formatInr(d.subscriptionRevenue);
    document.getElementById('kpiCardioRevenue').textContent = formatInr(d.cardioRevenue);

    // Render Recent Admissions List
    const admList = document.getElementById('recentAdmissionsList');
    if (d.recentAdmissions && d.recentAdmissions.length > 0) {
      admList.innerHTML = d.recentAdmissions.map(m => `
        <div class="flex items-center gap-3 p-2.5 rounded-xl bg-slate-900/60 border border-gym-border">
          <img src="${m.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}" class="w-10 h-10 rounded-xl object-cover border border-gym-border">
          <div class="flex-1 min-w-0">
            <p class="text-xs font-bold text-white truncate">${m.fullName}</p>
            <p class="text-[10px] text-slate-400 font-mono">${m.memberCode} • ${m.subscriptionPlan}</p>
          </div>
          <span class="text-[10px] font-bold px-2 py-0.5 rounded ${getStatusBadgeClass(m.status)}">${m.status}</span>
        </div>
      `).join('');
    } else {
      admList.innerHTML = '<p class="text-xs text-slate-500 text-center py-4">No admissions yet</p>';
    }

    // Render Recent Payments Table
    const payBody = document.getElementById('recentPaymentsTableBody');
    if (d.recentPayments && d.recentPayments.length > 0) {
      payBody.innerHTML = d.recentPayments.map(p => `
        <tr class="hover:bg-slate-800/40 transition">
          <td class="p-3 font-mono text-gym-orange">${p.receiptNumber}</td>
          <td class="p-3 font-bold text-white">${p.member ? p.member.fullName : 'N/A'}</td>
          <td class="p-3">${p.subscriptionPlan}</td>
          <td class="p-3 font-display font-bold text-white">${formatInr(p.amount)}</td>
          <td class="p-3"><span class="px-2 py-0.5 rounded bg-slate-800 text-[10px] font-bold">${p.paymentMethod}</span></td>
          <td class="p-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${getPaymentStatusBadge(p.paymentStatus)}">${p.paymentStatus}</span></td>
          <td class="p-3 text-slate-400">${p.paymentDate}</td>
        </tr>
      `).join('');
    }

    // Render Charts
    renderDashboardCharts(d);

  } catch (e) {
    console.error('Error loading admin dashboard', e);
  }
}

function renderDashboardCharts(d) {
  if (typeof Chart === 'undefined') {
    return;
  }
  try {
    // Chart 1: Plan Distribution (Doughnut)
    const ctxPlans = document.getElementById('chartPlans');
    if (ctxPlans) {
      if (STATE.charts.plans) STATE.charts.plans.destroy();
      const plans = d.planDistribution || { '1 Month': 2, '3 Months': 1, '6 Months': 1, '1 Year': 1 };
      STATE.charts.plans = new Chart(ctxPlans, {
        type: 'doughnut',
        data: {
          labels: Object.keys(plans),
          datasets: [{
            data: Object.values(plans),
            backgroundColor: ['#ff5722', '#f59e0b', '#10b981', '#06b6d4'],
            borderWidth: 2,
            borderColor: '#101622'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'bottom', labels: { color: '#94a3b8', font: { size: 10 } } }
          }
        }
      });
    }

    // Chart 2: Batch Distribution (Bar)
    const ctxBatches = document.getElementById('chartBatches');
    if (ctxBatches) {
      if (STATE.charts.batches) STATE.charts.batches.destroy();
      STATE.charts.batches = new Chart(ctxBatches, {
        type: 'bar',
        data: {
          labels: ['Morning Batch', 'Evening Batch', 'Cardio', 'Strength'],
          datasets: [{
            label: 'Athletes',
            data: [d.morningBatchMembers || 0, d.eveningBatchMembers || 0, d.cardioMembers || 0, d.strengthMembers || 0],
            backgroundColor: ['#ff5722', '#f59e0b', '#06b6d4', '#10b981'],
            borderRadius: 8
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: { grid: { color: '#1f293d' }, ticks: { color: '#94a3b8', stepSize: 1 } },
            x: { grid: { display: false }, ticks: { color: '#94a3b8', font: { size: 10 } } }
          },
          plugins: {
            legend: { display: false }
          }
        }
      });
    }
  } catch (err) {
    console.warn('Charts render skipped', err);
  }
}

// ================= ADMISSION & PRICING =================
function updatePhotoPreview(url) {
  const preview = document.getElementById('admissionPhotoPreview');
  if (url && url.trim().length > 5) {
    preview.src = url.trim();
  }
}

function setPresetPhoto(url) {
  document.getElementById('admissionPhotoUrl').value = url;
  updatePhotoPreview(url);
}

function recalculateAdmissionFee() {
  const selectedPlan = document.querySelector('input[name="subPlan"]:checked')?.value || '1 Month';
  const hasCardio = document.getElementById('admissionCardioOption')?.checked || false;

  const basePriceMap = {
    '1 Month': 800,
    '3 Months': 1800,
    '6 Months': 3500,
    '1 Year': 6800
  };

  const baseFee = basePriceMap[selectedPlan] || 800;
  const cardioFee = hasCardio ? 500 : 0;
  const total = baseFee + cardioFee;

  document.getElementById('admissionFinalAmount').textContent = formatInr(total);
  document.getElementById('admissionCalculationBreakdown').textContent =
    `Plan: ₹${baseFee.toLocaleString()} + Cardio: ₹${cardioFee.toLocaleString()}`;
}


// ================= CAMERA CAPTURE ENGINE =================
let memberCameraStream = null;
let capturedMemberPhotoData = null;

async function openMemberCamera() {
  const video = document.getElementById('admissionCameraVideo');
  const preview = document.getElementById('admissionPhotoPreview');
  const liveIndicator = document.getElementById('liveCameraIndicator');
  const statusBadge = document.getElementById('cameraStatusBadge');
  const statusMsg = document.getElementById('cameraStatusMsg');
  const btnOpen = document.getElementById('btnOpenCamera');
  const btnCapture = document.getElementById('btnCapturePhoto');
  const btnRetake = document.getElementById('btnRetakePhoto');
  const btnUse = document.getElementById('btnUsePhoto');

  statusMsg.textContent = 'Requesting camera access...';

  try {
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
      throw new Error('Camera API not supported in this browser.');
    }

    stopMemberCamera();

    memberCameraStream = await navigator.mediaDevices.getUserMedia({
      video: {
        width: { ideal: 640 },
        height: { ideal: 480 },
        facingMode: 'user'
      },
      audio: false
    });

    video.srcObject = memberCameraStream;
    video.classList.remove('hidden');
    preview.classList.add('hidden');
    liveIndicator.classList.remove('hidden');

    statusBadge.textContent = '● LIVE CAMERA';
    statusBadge.className = 'text-[10px] px-2.5 py-0.5 rounded-full bg-red-950/80 text-red-400 border border-red-800 font-mono';
    statusMsg.textContent = 'Position athlete face inside frame and click "Capture Photo".';

    btnOpen.classList.add('hidden');
    btnCapture.classList.remove('hidden');
    btnRetake.classList.add('hidden');
    btnUse.classList.add('hidden');
  } catch (err) {
    console.error('Camera access error:', err);
    statusBadge.textContent = 'CAMERA ERROR';
    statusBadge.className = 'text-[10px] px-2.5 py-0.5 rounded-full bg-amber-950/80 text-amber-400 border border-amber-800 font-mono';

    if (err.name === 'NotAllowedError' || err.name === 'PermissionDeniedError') {
      statusMsg.textContent = 'Camera permission denied. Please allow camera access or upload photo below.';
      showToast('Camera permission denied. Please allow camera access.', 'error');
    } else if (err.name === 'NotFoundError' || err.name === 'DevicesNotFoundError') {
      statusMsg.textContent = 'No camera found on this device. Please upload an image file below.';
      showToast('No camera found on this device.', 'error');
    } else {
      statusMsg.textContent = 'Unable to access camera: ' + (err.message || 'Unknown error');
      showToast('Unable to capture photo.', 'error');
    }
  }
}

function captureMemberPhoto() {
  const video = document.getElementById('admissionCameraVideo');
  const canvas = document.getElementById('admissionCameraCanvas');
  const preview = document.getElementById('admissionPhotoPreview');
  const liveIndicator = document.getElementById('liveCameraIndicator');
  const statusBadge = document.getElementById('cameraStatusBadge');
  const statusMsg = document.getElementById('cameraStatusMsg');
  const btnCapture = document.getElementById('btnCapturePhoto');
  const btnRetake = document.getElementById('btnRetakePhoto');
  const btnUse = document.getElementById('btnUsePhoto');

  if (!memberCameraStream) return;

  const w = video.videoWidth || 640;
  const h = video.videoHeight || 480;
  const size = Math.min(w, h);
  const startX = (w - size) / 2;
  const startY = (h - size) / 2;

  canvas.width = 400;
  canvas.height = 400;
  const ctx = canvas.getContext('2d');
  ctx.drawImage(video, startX, startY, size, size, 0, 0, 400, 400);

  capturedMemberPhotoData = canvas.toDataURL('image/jpeg', 0.92);
  preview.src = capturedMemberPhotoData;

  video.classList.add('hidden');
  preview.classList.remove('hidden');
  liveIndicator.classList.add('hidden');

  statusBadge.textContent = 'PHOTO CAPTURED';
  statusBadge.className = 'text-[10px] px-2.5 py-0.5 rounded-full bg-cyan-950/80 text-cyan-400 border border-cyan-800 font-mono';
  statusMsg.textContent = 'Review captured photograph. Click "Use Photo" to confirm or "Retake" to capture again.';

  btnCapture.classList.add('hidden');
  btnRetake.classList.remove('hidden');
  btnUse.classList.remove('hidden');
}

function retakeMemberPhoto() {
  const video = document.getElementById('admissionCameraVideo');
  const preview = document.getElementById('admissionPhotoPreview');
  const liveIndicator = document.getElementById('liveCameraIndicator');
  const statusBadge = document.getElementById('cameraStatusBadge');
  const statusMsg = document.getElementById('cameraStatusMsg');
  const btnCapture = document.getElementById('btnCapturePhoto');
  const btnRetake = document.getElementById('btnRetakePhoto');
  const btnUse = document.getElementById('btnUsePhoto');

  video.classList.remove('hidden');
  preview.classList.add('hidden');
  liveIndicator.classList.remove('hidden');

  statusBadge.textContent = '● LIVE CAMERA';
  statusBadge.className = 'text-[10px] px-2.5 py-0.5 rounded-full bg-red-950/80 text-red-400 border border-red-800 font-mono';
  statusMsg.textContent = 'Position athlete face inside frame and click "Capture Photo".';

  btnCapture.classList.remove('hidden');
  btnRetake.classList.add('hidden');
  btnUse.classList.add('hidden');
}

async function confirmMemberPhoto() {
  const statusBadge = document.getElementById('cameraStatusBadge');
  const statusMsg = document.getElementById('cameraStatusMsg');
  const btnOpen = document.getElementById('btnOpenCamera');
  const btnRetake = document.getElementById('btnRetakePhoto');
  const btnUse = document.getElementById('btnUsePhoto');

  if (!capturedMemberPhotoData) {
    showToast('Unable to capture photo.', 'error');
    return;
  }

  statusMsg.textContent = 'Saving captured photo to server...';

  try {
    const res = await fetch('/api/upload/member-photo', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({ base64Image: capturedMemberPhotoData })
    });

    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Unable to capture photo.', 'error');
      statusMsg.textContent = data.error || 'Unable to capture photo.';
      return;
    }

    document.getElementById('admissionPhotoUrl').value = data.url;
    stopMemberCamera();

    statusBadge.textContent = '✓ CONFIRMED';
    statusBadge.className = 'text-[10px] px-2.5 py-0.5 rounded-full bg-emerald-950/80 text-emerald-400 border border-emerald-800 font-mono';
    statusMsg.textContent = 'Member photo saved successfully and linked to admission.';
    showToast('Member photo saved successfully.', 'success');

    btnRetake.classList.add('hidden');
    btnUse.classList.add('hidden');
    btnOpen.classList.remove('hidden');
    btnOpen.querySelector('span').textContent = 'Re-take with Camera';
  } catch (err) {
    console.error('Error saving member photo:', err);
    showToast('Unable to capture photo.', 'error');
    statusMsg.textContent = 'Network error saving captured photo.';
  }
}

function stopMemberCamera() {
  if (memberCameraStream) {
    memberCameraStream.getTracks().forEach(track => track.stop());
    memberCameraStream = null;
  }
  const video = document.getElementById('admissionCameraVideo');
  if (video) video.srcObject = null;
}

async function handleMemberFileSelect(e) {
  const file = e.target.files[0];
  if (!file) return;

  const preview = document.getElementById('admissionPhotoPreview');
  const statusBadge = document.getElementById('cameraStatusBadge');
  const statusMsg = document.getElementById('cameraStatusMsg');

  preview.src = URL.createObjectURL(file);
  preview.classList.remove('hidden');
  document.getElementById('admissionCameraVideo').classList.add('hidden');
  document.getElementById('liveCameraIndicator').classList.add('hidden');
  stopMemberCamera();

  statusMsg.textContent = 'Uploading photo from device...';

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await fetch('/api/upload/member-photo', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${STATE.token}` },
      body: formData
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Photo upload failed', 'error');
      statusMsg.textContent = data.error || 'Photo upload failed';
      return;
    }

    document.getElementById('admissionPhotoUrl').value = data.url;
    statusBadge.textContent = '✓ UPLOADED';
    statusBadge.className = 'text-[10px] px-2.5 py-0.5 rounded-full bg-emerald-950/80 text-emerald-400 border border-emerald-800 font-mono';
    statusMsg.textContent = 'Member photo saved successfully.';
    showToast('Member photo saved successfully.', 'success');
  } catch (err) {
    console.error('File upload error:', err);
    showToast('Unable to upload photo.', 'error');
    statusMsg.textContent = 'Network error during upload.';
  }
}

async function handleSupplementFileSelect(e) {
  const file = e.target.files[0];
  if (!file) return;

  const preview = document.getElementById('suppPreviewImg');
  const msg = document.getElementById('suppUploadMsg');

  preview.src = URL.createObjectURL(file);
  msg.textContent = 'Uploading supplement image...';

  const formData = new FormData();
  formData.append('file', file);

  try {
    const res = await fetch('/api/upload/supplement-photo', {
      method: 'POST',
      headers: { 'Authorization': `Bearer ${STATE.token}` },
      body: formData
    });
    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Supplement image upload failed.', 'error');
      msg.textContent = data.error || 'Upload failed.';
      return;
    }

    document.getElementById('suppImage').value = data.url;
    msg.textContent = 'Image ready: ' + file.name;
    showToast('Supplement image upload succeeded.', 'success');
  } catch (err) {
    console.error('Supplement upload error:', err);
    showToast('Supplement image upload failed.', 'error');
    msg.textContent = 'Upload failed due to network error.';
  }
}

async function handleAdmissionSubmit(e) {
  e.preventDefault();
  const selectedPlan = document.querySelector('input[name="subPlan"]:checked')?.value || '1 Month';
  const hasCardio = document.getElementById('admissionCardioOption')?.checked || false;

  const payload = {
    fullName: document.getElementById('admissionFullName').value.trim(),
    phoneNumber: document.getElementById('admissionPhone').value.trim(),
    photoUrl: document.getElementById('admissionPhotoUrl').value.trim() || document.getElementById('admissionPhotoPreview').src,
    admissionDate: document.getElementById('admissionDate').value,
    subscriptionPlan: selectedPlan,
    trainingCategory: document.getElementById('admissionCategory').value,
    batch: document.getElementById('admissionBatch').value,
    cardioOption: hasCardio,
    paymentMethod: document.getElementById('admissionPaymentMethod').value,
    paymentStatus: document.getElementById('admissionPaymentStatus').value,
    transactionRef: document.getElementById('admissionTxnRef').value.trim(),
    notes: document.getElementById('admissionNotes').value.trim()
  };

  try {
    const res = await fetch('/api/admin/members', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify(payload)
    });

    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Admission failed', 'error');
      return;
    }

    showToast(`Athlete ${data.fullName} enrolled as ${data.memberCode}!`, 'success');
    document.getElementById('admissionForm').reset();
    initDateInputs();
    recalculateAdmissionFee();
    navigateTo('members');
  } catch (err) {
    showToast('Network error during admission', 'error');
  }
}

// ================= MEMBERS MANAGEMENT =================
let searchTimeout;
function debounceMemberSearch() {
  clearTimeout(searchTimeout);
  searchTimeout = setTimeout(() => {
    loadMembers();
  }, 300);
}

async function loadMembers() {
  const q = document.getElementById('memberSearchInput')?.value.trim() || '';
  const batch = document.getElementById('memberBatchFilter')?.value || 'All';
  const category = document.getElementById('memberCategoryFilter')?.value || 'All';
  const status = document.getElementById('memberStatusFilter')?.value || 'All';

  const params = new URLSearchParams();
  if (q) params.append('q', q);
  if (batch !== 'All') params.append('batch', batch);
  if (category !== 'All') params.append('category', category);
  if (status !== 'All') params.append('status', status);

  try {
    const res = await fetch(`/api/admin/members?${params.toString()}`, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const members = await res.json();
    STATE.members = members;

    const tbody = document.getElementById('membersTableBody');
    if (members.length === 0) {
      tbody.innerHTML = '<tr><td colspan="8" class="text-center py-8 text-slate-500">No members match your criteria</td></tr>';
      return;
    }

    tbody.innerHTML = members.map(m => `
      <tr class="hover:bg-slate-800/50 transition">
        <td class="p-3.5">
          <div class="flex items-center gap-3">
            <img src="${m.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}" class="w-10 h-10 rounded-xl object-cover border border-gym-border">
            <div>
              <p class="font-bold text-white">${m.fullName}</p>
              <p class="text-[11px] text-slate-400">Admitted: ${m.admissionDate}</p>
            </div>
          </div>
        </td>
        <td class="p-3.5 font-mono text-gym-orange font-bold">${m.memberCode}</td>
        <td class="p-3.5 text-slate-300 font-mono">${m.phoneNumber}</td>
        <td class="p-3.5 font-semibold text-white">
          ${m.subscriptionPlan}
          ${m.hasCardio ? '<span class="ml-1 text-[10px] text-cyan-400 font-bold">+Cardio</span>' : ''}
        </td>
        <td class="p-3.5">
          <span class="block text-slate-200">${m.trainingCategory}</span>
          <span class="block text-[10px] text-slate-400">${m.batch}</span>
        </td>
        <td class="p-3.5">
          <span class="block text-slate-200 font-medium">${m.expiryDate}</span>
          <span class="block text-[11px] ${getDaysRemainingColor(m.daysRemaining)} font-bold">
            ${m.daysRemaining < 0 ? 'Expired' : m.daysRemaining + ' Days Left'}
          </span>
        </td>
        <td class="p-3.5">
          <span class="px-2.5 py-1 rounded-md text-[10px] font-bold ${getStatusBadgeClass(m.status)}">
            ${m.status}
          </span>
        </td>
        <td class="p-3.5 text-right space-x-1">
          <button onclick="viewMemberDetail(${m.id})" title="View Profile" class="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg">👁️</button>
          <button onclick="openRenewModalForMember(${m.id}, '${escapeHtml(m.fullName)}', '${m.memberCode}', '${m.expiryDate}', '${m.subscriptionPlan}')" title="Renew" class="p-1.5 bg-gym-orange/20 hover:bg-gym-orange text-gym-orange hover:text-white rounded-lg transition">🔄</button>
          <button onclick="deleteMember(${m.id}, '${escapeHtml(m.fullName)}')" title="Delete" class="p-1.5 bg-red-950/40 hover:bg-gym-crimson text-gym-crimson hover:text-white rounded-lg transition">🗑️</button>
        </td>
      </tr>
    `).join('');
  } catch (e) {
    console.error('Error loading members', e);
  }
}

async function viewMemberDetail(id) {
  try {
    const res = await fetch(`/api/admin/members/${id}`, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const d = await res.json();
    const m = d.member;
    const payments = d.payments || [];

    const content = document.getElementById('memberProfileContent');
    content.innerHTML = `
      <div class="flex items-center gap-5 p-4 rounded-xl bg-slate-900 border border-gym-border">
        <img src="${m.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400'}" class="w-20 h-20 rounded-2xl object-cover border-2 border-gym-orange">
        <div class="space-y-1">
          <h4 class="font-display font-bold text-2xl uppercase text-white">${m.fullName}</h4>
          <p class="font-mono text-xs text-gym-orange font-bold">Member Code: ${m.memberCode}</p>
          <p class="text-xs text-slate-400">Phone: ${m.phoneNumber} | Admitted: ${m.admissionDate}</p>
          <span class="inline-block px-2.5 py-0.5 rounded text-[10px] font-bold ${getStatusBadgeClass(m.status)}">${m.status}</span>
        </div>
      </div>

      <div class="grid grid-cols-2 sm:grid-cols-4 gap-3 text-xs">
        <div class="bg-slate-900 p-3 rounded-xl border border-gym-border">
          <span class="text-slate-400 block">Plan</span>
          <span class="font-bold text-white">${m.subscriptionPlan}</span>
        </div>
        <div class="bg-slate-900 p-3 rounded-xl border border-gym-border">
          <span class="text-slate-400 block">Cardio Access</span>
          <span class="font-bold ${m.hasCardio ? 'text-cyan-400' : 'text-slate-400'}">${m.hasCardio ? 'Yes (+₹500)' : 'No'}</span>
        </div>
        <div class="bg-slate-900 p-3 rounded-xl border border-gym-border">
          <span class="text-slate-400 block">Batch</span>
          <span class="font-bold text-white">${m.batch}</span>
        </div>
        <div class="bg-slate-900 p-3 rounded-xl border border-gym-border">
          <span class="text-slate-400 block">Category</span>
          <span class="font-bold text-white">${m.trainingCategory}</span>
        </div>
      </div>

      <div class="p-4 rounded-xl bg-slate-900 border border-gym-border text-xs space-y-1">
        <div class="flex justify-between">
          <span class="text-slate-400">Duration:</span>
          <span class="text-white font-semibold">${m.startDate} to ${m.expiryDate}</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Days Remaining:</span>
          <span class="font-bold ${getDaysRemainingColor(d.daysRemaining)}">${d.daysRemaining} Days</span>
        </div>
        <div class="flex justify-between">
          <span class="text-slate-400">Status Alert:</span>
          <span class="font-bold text-white">${d.statusBadgeText}</span>
        </div>
        ${m.notes ? `<div class="pt-2 border-t border-gym-border/60 text-slate-300"><strong>Notes:</strong> ${m.notes}</div>` : ''}
      </div>

      <div>
        <h5 class="font-display font-bold text-sm uppercase text-white mb-2">Payment History</h5>
        <div class="max-h-40 overflow-y-auto space-y-2 text-xs">
          ${payments.map(p => `
            <div class="flex items-center justify-between p-2.5 rounded-lg bg-slate-900/60 border border-gym-border">
              <div>
                <span class="font-mono text-gym-orange block">${p.receiptNumber}</span>
                <span class="text-slate-400 text-[10px]">${p.paymentDate} via ${p.paymentMethod}</span>
              </div>
              <div class="text-right">
                <span class="font-display font-bold text-white block">${formatInr(p.amount)}</span>
                <span class="text-[10px] font-bold ${getPaymentStatusBadge(p.paymentStatus)}">${p.paymentStatus}</span>
              </div>
            </div>
          `).join('')}
        </div>
      </div>
    `;

    document.getElementById('memberProfileModal').classList.remove('hidden');
  } catch (e) {
    showToast('Failed to load member profile', 'error');
  }
}

function closeMemberProfileModal() {
  document.getElementById('memberProfileModal').classList.add('hidden');
}

async function deleteMember(id, name) {
  if (!confirm(`Are you sure you want to deactivate and remove member ${name}?`)) return;
  try {
    const res = await fetch(`/api/admin/members/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (res.ok) {
      showToast(`Member ${name} removed`, 'info');
      loadMembers();
    } else {
      showToast('Could not delete member', 'error');
    }
  } catch (e) {
    showToast('Network error', 'error');
  }
}

// ================= RENEWALS MANAGEMENT =================
function filterRenewals(status) {
  STATE.renewalsFilter = status;
  ['All', 'ACTIVE', 'EXPIRING_SOON', 'EXPIRED'].forEach(s => {
    const btn = document.getElementById(`renewalTab${s === 'All' ? 'All' : s === 'ACTIVE' ? 'Active' : s === 'EXPIRING_SOON' ? 'Expiring' : 'Expired'}`);
    if (btn) {
      if (s === status) {
        btn.className = 'px-3 py-1.5 rounded-lg bg-gym-orange text-white';
      } else {
        btn.className = 'px-3 py-1.5 rounded-lg text-slate-400 hover:text-white';
      }
    }
  });
  loadRenewals();
}

async function loadRenewals() {
  try {
    const params = new URLSearchParams();
    if (STATE.renewalsFilter !== 'All') {
      params.append('status', STATE.renewalsFilter);
    }
    const res = await fetch(`/api/admin/members?${params.toString()}`, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const members = await res.json();

    const tbody = document.getElementById('renewalsTableBody');
    if (members.length === 0) {
      tbody.innerHTML = '<tr><td colspan="7" class="text-center py-8 text-slate-500">No members in this renewal category</td></tr>';
      return;
    }

    tbody.innerHTML = members.map(m => `
      <tr class="hover:bg-slate-800/50 transition">
        <td class="p-3.5">
          <div class="flex items-center gap-3">
            <img src="${m.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}" class="w-10 h-10 rounded-xl object-cover border border-gym-border">
            <div>
              <p class="font-bold text-white">${m.fullName}</p>
              <p class="text-[11px] font-mono text-gym-orange">${m.memberCode}</p>
            </div>
          </div>
        </td>
        <td class="p-3.5 font-mono text-slate-300">${m.phoneNumber}</td>
        <td class="p-3.5 font-semibold text-white">${m.subscriptionPlan}</td>
        <td class="p-3.5 font-medium text-slate-300">${m.expiryDate}</td>
        <td class="p-3.5">
          <span class="font-bold ${getDaysRemainingColor(m.daysRemaining)}">
            ${m.daysRemaining < 0 ? 'Expired (' + Math.abs(m.daysRemaining) + 'd ago)' : m.daysRemaining + ' Days Left'}
          </span>
        </td>
        <td class="p-3.5">
          <span class="px-2.5 py-1 rounded-md text-[10px] font-bold ${getStatusBadgeClass(m.status)}">
            ${m.status === 'EXPIRING_SOON' ? '⚠️ Renewal Required' : m.status === 'EXPIRED' ? '❌ Subscription Expired' : 'Active'}
          </span>
        </td>
        <td class="p-3.5 text-right">
          <button onclick="openRenewModalForMember(${m.id}, '${escapeHtml(m.fullName)}', '${m.memberCode}', '${m.expiryDate}', '${m.subscriptionPlan}')" class="bg-gradient-to-r from-gym-orange to-red-600 hover:from-gym-orangeHover hover:to-red-700 text-white px-3.5 py-1.5 rounded-lg text-xs font-bold uppercase shadow-md transition">
            Renew
          </button>
        </td>
      </tr>
    `).join('');
  } catch (e) {
    console.error('Error loading renewals', e);
  }
}

function openRenewModalForMember(id, name, code, expiry, currentPlan) {
  STATE.selectedRenewMemberId = id;
  document.getElementById('renewMemberName').textContent = name;
  document.getElementById('renewMemberCode').textContent = code;
  document.getElementById('renewCurrentExpiry').textContent = expiry;
  document.getElementById('renewPlanSelect').value = currentPlan || '1 Month';
  document.getElementById('renewCardioOption').checked = false;
  recalcRenewTotal();
  document.getElementById('renewModal').classList.remove('hidden');
}

function closeRenewModal() {
  document.getElementById('renewModal').classList.add('hidden');
  STATE.selectedRenewMemberId = null;
}

function recalcRenewTotal() {
  const plan = document.getElementById('renewPlanSelect').value;
  const hasCardio = document.getElementById('renewCardioOption').checked;
  const priceMap = { '1 Month': 800, '3 Months': 1800, '6 Months': 3500, '1 Year': 6800 };
  const base = priceMap[plan] || 800;
  const total = base + (hasCardio ? 500 : 0);
  document.getElementById('renewFinalAmount').textContent = formatInr(total);
}

async function submitRenewal() {
  if (!STATE.selectedRenewMemberId) return;
  const plan = document.getElementById('renewPlanSelect').value;
  const hasCardio = document.getElementById('renewCardioOption').checked;
  const method = document.getElementById('renewPaymentMethod').value;
  const ref = document.getElementById('renewTxnRef').value.trim();

  try {
    const res = await fetch(`/api/admin/members/${STATE.selectedRenewMemberId}/renew`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({
        subscriptionPlan: plan,
        cardioOption: hasCardio,
        paymentMethod: method,
        paymentStatus: 'Paid',
        transactionRef: ref
      })
    });

    if (!res.ok) {
      showToast('Renewal failed', 'error');
      return;
    }

    const updated = await res.json();
    showToast(`Membership renewed until ${updated.expiryDate}!`, 'success');
    closeRenewModal();
    if (STATE.activeView === 'renewals') loadRenewals();
    else if (STATE.activeView === 'members') loadMembers();
  } catch (e) {
    showToast('Network error during renewal', 'error');
  }
}

// ================= PAYMENTS SECTION =================
let paymentSearchTimeout = null;
function debouncePaymentSearch() {
  clearTimeout(paymentSearchTimeout);
  paymentSearchTimeout = setTimeout(() => {
    loadPayments();
  }, 300);
}

function navigateToSupplementOrder(orderNumber) {
  showView('viewSupplementOrders');
  const searchInput = document.getElementById('orderSearchInput');
  if (searchInput) {
    searchInput.value = orderNumber;
    loadSupplementOrders();
  }
}

async function loadPayments() {
  const type = document.getElementById('paymentTypeFilter')?.value || 'All';
  const method = document.getElementById('paymentMethodFilter')?.value || 'All';
  const status = document.getElementById('paymentStatusFilter')?.value || 'All';
  const q = document.getElementById('paymentSearchInput')?.value.trim() || '';

  const params = new URLSearchParams();
  if (type !== 'All') params.append('type', type);
  if (method !== 'All') params.append('method', method);
  if (status !== 'All') params.append('status', status);
  if (q) params.append('q', q);

  try {
    const res = await fetch(`/api/admin/payments?${params.toString()}`, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const d = await res.json();

    if (document.getElementById('paymentsTotalRev')) {
      document.getElementById('paymentsTotalRev').textContent = formatInr(d.totalRevenue);
    }
    if (document.getElementById('paymentsSubRev')) {
      document.getElementById('paymentsSubRev').textContent = formatInr(d.subscriptionRevenue);
    }
    if (document.getElementById('paymentsCardioRev')) {
      document.getElementById('paymentsCardioRev').textContent = formatInr(d.cardioRevenue);
    }
    if (document.getElementById('paymentsSupplementRev')) {
      document.getElementById('paymentsSupplementRev').textContent = formatInr(d.supplementRevenue || 0);
    }

    const tbody = document.getElementById('paymentsTableBody');
    if (!tbody) return;

    if (!d.payments || d.payments.length === 0) {
      tbody.innerHTML = '<tr><td colspan="10" class="text-center py-8 text-slate-500">No payment transactions found</td></tr>';
      return;
    }

    tbody.innerHTML = d.payments.map(p => {
      const isSupplement = p.paymentType === 'SUPPLEMENT';

      // Type badge
      const typeBadge = isSupplement
        ? '<span class="px-2 py-0.5 rounded bg-amber-500/20 text-amber-400 border border-amber-500/40 text-[10px] font-bold inline-flex items-center gap-1"><span>🟠</span> SUPPLEMENT</span>'
        : '<span class="px-2 py-0.5 rounded bg-blue-500/20 text-blue-400 border border-blue-500/40 text-[10px] font-bold">MEMBERSHIP</span>';

      // Batch badge
      const isEvening = (p.batch && p.batch.toLowerCase().includes('evening')) ||
                        (p.member && p.member.batch && p.member.batch.toLowerCase().includes('evening'));
      const batchBadge = isEvening
        ? '<span class="px-2 py-0.5 rounded bg-indigo-500/20 text-indigo-300 border border-indigo-500/40 text-[10px] font-bold inline-flex items-center gap-1">🌆 EVENING</span>'
        : '<span class="px-2 py-0.5 rounded bg-amber-500/20 text-amber-300 border border-amber-500/40 text-[10px] font-bold inline-flex items-center gap-1">🌅 MORNING</span>';

      // Member details
      const memberName = p.member ? p.member.fullName : (p.memberName || 'Athlete / Guest');
      const memberCode = p.member ? p.member.memberCode : (p.memberCode || '');
      const memberPhone = p.member ? p.member.phoneNumber : (p.memberPhone || '-');

      // Payment Details
      let detailsHtml = '';
      if (isSupplement) {
        const prodName = p.productName || (p.supplementOrder && p.supplementOrder.supplement ? p.supplementOrder.supplement.name : 'Supplement Item');
        const orderNum = p.orderNumber || (p.supplementOrder ? p.supplementOrder.orderNumber : '-');
        detailsHtml = `
          <div>
            <span class="font-bold text-white text-xs block">${prodName} <span class="text-gym-orange">×${p.quantity || 1}</span></span>
            <span class="text-[10px] text-slate-400 font-mono">Order: <span class="text-gym-orange font-semibold">${orderNum}</span></span>
            ${p.originalPrice > p.finalPrice && p.finalPrice > 0 ? `
              <span class="block text-[10px] text-gym-emerald font-mono">₹${Math.round(p.finalPrice)}/unit <span class="line-through text-slate-500">₹${Math.round(p.originalPrice)}</span></span>
            ` : ''}
          </div>
        `;
      } else {
        detailsHtml = `
          <div>
            <span class="text-slate-300 font-medium block">${p.subscriptionPlan || 'Membership'} (${formatInr(p.baseFee)})</span>
            ${p.cardioFee > 0 ? `<span class="text-[10px] text-cyan-400 font-mono block">+ Cardio: ${formatInr(p.cardioFee)}</span>` : ''}
          </div>
        `;
      }

      // Method & UTR
      const methodHtml = `
        <div>
          <span class="px-2 py-0.5 rounded bg-slate-800 text-[10px] font-bold text-slate-300">${p.paymentMethod || 'UPI'}</span>
          ${p.transactionRef ? `<span class="block text-[10px] text-slate-400 font-mono mt-1" title="UTR / Transaction Ref">Ref: ${p.transactionRef}</span>` : ''}
        </div>
      `;

      return `
        <tr class="hover:bg-slate-800/50 transition">
          <td class="p-3.5 font-mono text-gym-orange font-bold whitespace-nowrap">${p.receiptNumber}</td>
          <td class="p-3.5 whitespace-nowrap">${typeBadge}</td>
          <td class="p-3.5 whitespace-nowrap">${batchBadge}</td>
          <td class="p-3.5">
            <div class="font-bold text-white">${memberName}</div>
            <div class="text-[10px] text-slate-400 font-mono">${memberCode ? memberCode + ' • ' : ''}${memberPhone}</div>
          </td>
          <td class="p-3.5">${detailsHtml}</td>
          <td class="p-3.5 font-display font-bold text-white text-sm whitespace-nowrap">${formatInr(p.amount)}</td>
          <td class="p-3.5 text-slate-400 whitespace-nowrap">${p.paymentDate}</td>
          <td class="p-3.5">${methodHtml}</td>
          <td class="p-3.5 whitespace-nowrap">
            <select onchange="changePaymentStatus(${p.id}, this.value)" class="bg-slate-900 border border-gym-border rounded px-2 py-1 text-[11px] font-bold ${getPaymentStatusColor(p.paymentStatus)}">
              <option value="Paid" ${p.paymentStatus === 'Paid' ? 'selected' : ''}>Paid</option>
              <option value="Pending" ${p.paymentStatus === 'Pending' ? 'selected' : ''}>Pending</option>
              <option value="Failed" ${p.paymentStatus === 'Failed' ? 'selected' : ''}>Failed</option>
            </select>
          </td>
          <td class="p-3.5 text-right whitespace-nowrap">
            <div class="flex items-center justify-end gap-1">
              <button onclick="printReceipt(${JSON.stringify(p).replace(/"/g, '&quot;')})" class="p-1.5 bg-slate-800 hover:bg-slate-700 text-slate-300 rounded-lg" title="Print Receipt">🖨️</button>
              ${isSupplement && (p.orderNumber || (p.supplementOrder && p.supplementOrder.orderNumber)) ? `
                <button onclick="navigateToSupplementOrder('${p.orderNumber || p.supplementOrder.orderNumber}')" class="p-1.5 bg-slate-800 hover:bg-slate-700 text-amber-400 rounded-lg" title="View Order in Supplement Orders">📦</button>
              ` : ''}
            </div>
          </td>
        </tr>
      `;
    }).join('');
  } catch (e) {
    console.error('Error loading payments', e);
  }
}

async function changePaymentStatus(id, newStatus) {
  try {
    const res = await fetch(`/api/admin/payments/${id}/status`, {
      method: 'PATCH',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({ status: newStatus })
    });
    if (res.ok) {
      showToast(`Payment status updated to ${newStatus}`, 'success');
      loadPayments();
      if (typeof loadDailyCollection === 'function') {
        loadDailyCollection();
      }
      if (typeof loadSupplementOrders === 'function') {
        loadSupplementOrders();
      }
    } else {
      showToast('Failed to update payment status', 'error');
    }
  } catch (e) {
    showToast('Failed to update payment status', 'error');
  }
}

// ================= DAILY COLLECTION FUNCTIONS =================
let isDailyBreakdownExpanded = true;

function toggleDailyBreakdown() {
  const content = document.getElementById('dailyBreakdownContent');
  const text = document.getElementById('breakdownToggleText');
  const icon = document.getElementById('breakdownToggleIcon');
  if (!content) return;

  isDailyBreakdownExpanded = !isDailyBreakdownExpanded;
  if (isDailyBreakdownExpanded) {
    content.classList.remove('hidden');
    if (text) text.textContent = 'Hide Details';
    if (icon) icon.classList.remove('rotate-180');
  } else {
    content.classList.add('hidden');
    if (text) text.textContent = 'Show Details';
    if (icon) icon.classList.add('rotate-180');
  }
}

function setDailyCollectionToday() {
  const today = new Date().toISOString().split('T')[0];
  const dateInput = document.getElementById('dailyCollectionDate');
  if (dateInput) {
    dateInput.value = today;
  }
  loadDailyCollection(today);
}

function selectDailyCollectionDate(dateStr) {
  const dateInput = document.getElementById('dailyCollectionDate');
  if (dateInput) {
    dateInput.value = dateStr;
  }
  loadDailyCollection(dateStr);
  window.scrollTo({ top: 0, behavior: 'smooth' });
}

async function loadDailyCollection(targetDate) {
  const dateInput = document.getElementById('dailyCollectionDate');
  const today = new Date().toISOString().split('T')[0];
  let selectedDate = targetDate || (dateInput ? dateInput.value : '') || today;

  if (dateInput && !dateInput.value) {
    dateInput.value = selectedDate;
  }

  const labelElem = document.getElementById('breakdownDateLabel');
  if (labelElem) labelElem.textContent = selectedDate === today ? `${selectedDate} (Today)` : selectedDate;

  const grandDateLabel = document.getElementById('dailyGrandTotalDate');
  if (grandDateLabel) grandDateLabel.textContent = selectedDate === today ? 'Today' : selectedDate;

  try {
    const res = await fetch(`/api/admin/payments/daily-collection?date=${selectedDate}`, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const d = await res.json();

    // 3 Main Cards
    if (document.getElementById('dailyMorningTotal')) {
      document.getElementById('dailyMorningTotal').textContent = formatInr(d.morningTotal || 0);
    }
    if (document.getElementById('dailyEveningTotal')) {
      document.getElementById('dailyEveningTotal').textContent = formatInr(d.eveningTotal || 0);
    }
    if (document.getElementById('dailyGrandTotal')) {
      document.getElementById('dailyGrandTotal').textContent = formatInr(d.grandTotal || 0);
    }

    const morningCount = (d.morningBreakdown && d.morningBreakdown.count) ? d.morningBreakdown.count : 0;
    const eveningCount = (d.eveningBreakdown && d.eveningBreakdown.count) ? d.eveningBreakdown.count : 0;

    if (document.getElementById('dailyMorningCount')) {
      document.getElementById('dailyMorningCount').textContent = `${morningCount} txn${morningCount === 1 ? '' : 's'}`;
    }
    if (document.getElementById('dailyEveningCount')) {
      document.getElementById('dailyEveningCount').textContent = `${eveningCount} txn${eveningCount === 1 ? '' : 's'}`;
    }
    if (document.getElementById('dailyGrandCount')) {
      document.getElementById('dailyGrandCount').textContent = `${morningCount + eveningCount} Total Verified`;
    }

    // Card previews
    const mMem = d.morningBreakdown ? d.morningBreakdown.membership : 0;
    const mSupp = d.morningBreakdown ? d.morningBreakdown.supplements : 0;
    const eMem = d.eveningBreakdown ? d.eveningBreakdown.membership : 0;
    const eSupp = d.eveningBreakdown ? d.eveningBreakdown.supplements : 0;

    if (document.getElementById('dailyMorningMemPreview')) document.getElementById('dailyMorningMemPreview').textContent = formatInr(mMem);
    if (document.getElementById('dailyMorningSuppPreview')) document.getElementById('dailyMorningSuppPreview').textContent = formatInr(mSupp);
    if (document.getElementById('dailyEveningMemPreview')) document.getElementById('dailyEveningMemPreview').textContent = formatInr(eMem);
    if (document.getElementById('dailyEveningSuppPreview')) document.getElementById('dailyEveningSuppPreview').textContent = formatInr(eSupp);

    // Breakdown details
    if (document.getElementById('breakdownMorningSum')) document.getElementById('breakdownMorningSum').textContent = formatInr(d.morningTotal || 0);
    if (document.getElementById('breakdownMorningMem')) document.getElementById('breakdownMorningMem').textContent = formatInr(mMem);
    if (document.getElementById('breakdownMorningSupp')) document.getElementById('breakdownMorningSupp').textContent = formatInr(mSupp);

    if (document.getElementById('breakdownEveningSum')) document.getElementById('breakdownEveningSum').textContent = formatInr(d.eveningTotal || 0);
    if (document.getElementById('breakdownEveningMem')) document.getElementById('breakdownEveningMem').textContent = formatInr(eMem);
    if (document.getElementById('breakdownEveningSupp')) document.getElementById('breakdownEveningSupp').textContent = formatInr(eSupp);

    // History Table
    const histBody = document.getElementById('dailyHistoryTableBody');
    if (histBody) {
      if (!d.history || d.history.length === 0) {
        histBody.innerHTML = '<tr><td colspan="5" class="text-center py-6 text-slate-500">No collection history recorded yet</td></tr>';
      } else {
        histBody.innerHTML = d.history.map(h => {
          const isSelected = h.date === selectedDate;
          const isToday = h.date === today;
          return `
            <tr class="hover:bg-slate-800/50 transition ${isSelected ? 'bg-gym-orange/5' : ''}">
              <td class="p-3.5 font-mono font-bold ${isSelected ? 'text-gym-orange' : 'text-white'}">
                ${h.date}
                ${isToday ? '<span class="text-[10px] bg-gym-orange/20 text-gym-orange border border-gym-orange/40 px-1.5 py-0.5 rounded font-bold ml-1.5">TODAY</span>' : ''}
              </td>
              <td class="p-3.5 font-mono text-amber-300 font-semibold">${formatInr(h.morningTotal)}</td>
              <td class="p-3.5 font-mono text-indigo-300 font-semibold">${formatInr(h.eveningTotal)}</td>
              <td class="p-3.5 font-mono text-emerald-400 font-bold text-sm">${formatInr(h.grandTotal)}</td>
              <td class="p-3.5 text-right">
                <button onclick="selectDailyCollectionDate('${h.date}')" class="px-2.5 py-1 bg-slate-800 hover:bg-slate-700 text-slate-200 rounded text-xs transition border border-gym-border ${isSelected ? 'border-gym-orange text-gym-orange font-bold' : ''}">
                  ${isSelected ? 'Selected' : 'View'}
                </button>
              </td>
            </tr>
          `;
        }).join('');
      }
    }
  } catch (err) {
    console.error('Error loading daily collection', err);
  }
}

function printReceipt(p) {
  const content = document.getElementById('receiptContent');
  if (!content) return;

  if (p.paymentType === 'SUPPLEMENT') {
    const memberName = p.member ? p.member.fullName : (p.memberName || 'Customer / Athlete');
    const memberCode = p.member ? p.member.memberCode : (p.memberCode || '');
    const orderNum = p.orderNumber || (p.supplementOrder ? p.supplementOrder.orderNumber : '-');
    const prodName = p.productName || (p.supplementOrder && p.supplementOrder.supplement ? p.supplementOrder.supplement.name : 'Supplement Item');
    const qty = p.quantity || 1;
    const unitPrice = p.finalPrice > 0 ? p.finalPrice : (p.amount / qty);

    content.innerHTML = `
      <div class="flex justify-between border-b pb-2">
        <span class="text-slate-500">Receipt No:</span>
        <span class="font-mono font-bold text-black">${p.receiptNumber}</span>
      </div>
      <div class="flex justify-between">
        <span class="text-slate-500">Date:</span>
        <span class="font-semibold text-black">${p.paymentDate}</span>
      </div>
      <div class="flex justify-between">
        <span class="text-slate-500">Customer:</span>
        <span class="font-bold text-black">${memberName}</span>
      </div>
      ${memberCode ? `
      <div class="flex justify-between">
        <span class="text-slate-500">Member ID:</span>
        <span class="font-mono font-bold text-black">${memberCode}</span>
      </div>` : ''}
      <div class="flex justify-between border-t pt-2">
        <span class="text-slate-500">Category:</span>
        <span class="font-bold text-orange-600 uppercase text-xs">Gym Supplement Store</span>
      </div>
      <div class="flex justify-between">
        <span class="text-slate-500">Order ID:</span>
        <span class="font-mono font-bold text-black">${orderNum}</span>
      </div>
      <div class="flex justify-between">
        <span class="text-slate-500">Product:</span>
        <span class="font-semibold text-black">${prodName}</span>
      </div>
      <div class="flex justify-between">
        <span class="text-slate-500">Quantity:</span>
        <span class="font-bold text-black">${qty}</span>
      </div>
      <div class="flex justify-between">
        <span class="text-slate-500">Unit Price:</span>
        <span class="font-semibold text-black">${formatInr(unitPrice)}</span>
      </div>
      <div class="flex justify-between border-t pt-2 text-base font-bold">
        <span>Total Paid:</span>
        <span class="text-orange-600">${formatInr(p.amount)}</span>
      </div>
      <div class="flex justify-between pt-1">
        <span class="text-slate-500">Payment Method:</span>
        <span class="font-semibold text-black">${p.paymentMethod}</span>
      </div>
      ${p.transactionRef ? `
      <div class="flex justify-between">
        <span class="text-slate-500">Ref / UTR:</span>
        <span class="font-mono text-slate-700">${p.transactionRef}</span>
      </div>` : ''}
      <div class="text-center pt-4 text-[10px] text-slate-400">
        PowerFitnessKurnool Supplement Store — Keep this receipt for front desk pickup!
      </div>
    `;
    document.getElementById('receiptModal').classList.remove('hidden');
    return;
  }

  // Standard Gym Membership Receipt
  content.innerHTML = `
    <div class="flex justify-between border-b pb-2">
      <span class="text-slate-500">Receipt No:</span>
      <span class="font-mono font-bold text-black">${p.receiptNumber}</span>
    </div>
    <div class="flex justify-between">
      <span class="text-slate-500">Date:</span>
      <span class="font-semibold text-black">${p.paymentDate}</span>
    </div>
    <div class="flex justify-between">
      <span class="text-slate-500">Member:</span>
      <span class="font-bold text-black">${p.member ? p.member.fullName : (p.memberName || 'Athlete')}</span>
    </div>
    <div class="flex justify-between">
      <span class="text-slate-500">PFK Code:</span>
      <span class="font-mono font-bold text-black">${p.member ? p.member.memberCode : (p.memberCode || '')}</span>
    </div>
    <div class="flex justify-between border-t pt-2">
      <span class="text-slate-500">Plan:</span>
      <span class="font-semibold text-black">${p.subscriptionPlan || 'Membership'}</span>
    </div>
    <div class="flex justify-between">
      <span class="text-slate-500">Base Fee:</span>
      <span class="font-semibold text-black">${formatInr(p.baseFee)}</span>
    </div>
    <div class="flex justify-between">
      <span class="text-slate-500">Cardio Surcharge:</span>
      <span class="font-semibold text-black">${formatInr(p.cardioFee)}</span>
    </div>
    <div class="flex justify-between border-t pt-2 text-base font-bold">
      <span>Total Paid:</span>
      <span class="text-orange-600">${formatInr(p.amount)}</span>
    </div>
    <div class="flex justify-between pt-1">
      <span class="text-slate-500">Payment Method:</span>
      <span class="font-semibold text-black">${p.paymentMethod}</span>
    </div>
    ${p.transactionRef ? `
    <div class="flex justify-between">
      <span class="text-slate-500">Ref / UTR:</span>
      <span class="font-mono text-slate-700">${p.transactionRef}</span>
    </div>` : ''}
    <div class="text-center pt-4 text-[10px] text-slate-400">
      Thank you for training with PowerFitnessKurnool!
    </div>
  `;
  document.getElementById('receiptModal').classList.remove('hidden');
}

function closeReceiptModal() {
  document.getElementById('receiptModal').classList.add('hidden');
}

// ================= SUPPLEMENTS =================
// ================= SUPPLEMENTS & OFFER ENGINE =================
function toggleOfferFields() {
  const hasOffer = document.getElementById('suppHasOffer').value === 'true';
  const container = document.getElementById('suppOfferFieldsContainer');
  if (hasOffer) {
    container.classList.remove('hidden');
    recalcOfferPreview();
  } else {
    container.classList.add('hidden');
  }
}

function recalcOfferPreview() {
  const orig = parseFloat(document.getElementById('suppOriginalPrice').value) || 0;
  const offerType = document.getElementById('suppOfferType').value;
  const discount = parseFloat(document.getElementById('suppDiscountValue').value) || 0;
  const preview = document.getElementById('suppOfferPricePreview');
  const badge = document.getElementById('suppDiscountBadgePreview');
  const label = document.getElementById('suppDiscountLabel');

  if (label) {
    label.textContent = offerType === 'PERCENTAGE' ? 'Discount (%) *' : 'Discount (₹) *';
  }

  if (orig <= 0) {
    preview.textContent = '₹0';
    badge.textContent = 'Enter Price';
    return;
  }

  if (discount <= 0) {
    preview.textContent = formatInr(Math.round(orig));
    badge.textContent = '0% OFF';
    return;
  }

  if (offerType === 'PERCENTAGE') {
    if (discount >= 100) {
      preview.textContent = 'Invalid %';
      badge.textContent = 'Must be < 100%';
      return;
    }
    const finalPrice = Math.round(orig * (1 - (discount / 100)));
    preview.textContent = formatInr(finalPrice);
    badge.textContent = `${Math.round(discount)}% OFF`;
  } else {
    if (discount >= orig) {
      preview.textContent = 'Invalid Discount';
      badge.textContent = 'Discount ≥ Price';
      return;
    }
    const finalPrice = Math.round(orig - discount);
    preview.textContent = formatInr(finalPrice);
    badge.textContent = `₹${Math.round(discount).toLocaleString('en-IN')} OFF`;
  }
}

async function loadSupplements() {
  try {
    const category = STATE.supplementsCategory;
    const q = STATE.supplementsSearch;
    let url = '/api/supplements?';
    if (category && category !== 'All') url += `category=${encodeURIComponent(category)}&`;
    if (q) url += `q=${encodeURIComponent(q)}&`;

    const res = await fetch(url);
    if (!res.ok) return;
    const items = await res.json();
    STATE.supplementsCache = items;

    const grid = document.getElementById('supplementsGrid');
    if (items.length === 0) {
      grid.innerHTML = '<p class="col-span-4 text-center py-12 text-slate-500">No supplements found in this category.</p>';
      return;
    }

    const isAdmin = STATE.user && STATE.user.role === 'ADMIN';

    // Show/hide Add Supplement button for Admin
    const addBtn = document.getElementById('adminAddSupplementBtn');
    if (addBtn) {
      if (isAdmin) addBtn.classList.remove('hidden');
      else addBtn.classList.add('hidden');
    }

    grid.innerHTML = items.map(s => {
      const origPrice = s.originalPrice > 0 ? s.originalPrice : s.price;
      const isOfferActive = s.hasOffer && s.offerPrice > 0 && (s.price < origPrice || s.offerPrice < origPrice);
      const effectivePrice = isOfferActive ? s.offerPrice : origPrice;
      const discountText = s.offerType === 'FIXED' 
        ? `₹${Math.round(s.discountValue).toLocaleString('en-IN')} OFF` 
        : `${Math.round(s.discountValue)}% OFF`;
      
      const stockQty = s.stockQuantity != null ? s.stockQuantity : 0;
      const isAvailable = s.stockStatus === 'AVAILABLE' && stockQty > 0;

      return `
        <div class="bg-gym-card border border-gym-border rounded-2xl overflow-hidden flex flex-col group hover:border-gym-orange/50 transition shadow-xl relative">
          <!-- Top Media -->
          <div class="h-48 bg-slate-900 relative overflow-hidden flex items-center justify-center">
            <img src="${s.imageUrl || 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=400'}" alt="${s.name}" class="w-full h-full object-cover group-hover:scale-105 transition duration-300">
            
            ${isOfferActive ? `
              <span class="absolute top-2.5 left-2.5 bg-gradient-to-r from-red-600 to-gym-orange text-white text-[10px] font-extrabold uppercase px-2.5 py-1 rounded-lg shadow-lg flex items-center gap-1">
                <span>🔥</span> SPECIAL OFFER
              </span>
            ` : ''}

            <span class="absolute top-2.5 right-2.5 px-2.5 py-1 rounded-md text-[10px] font-bold ${isAvailable ? 'bg-gym-emerald/90 text-white' : 'bg-gym-crimson/90 text-white'}">
              ${isAvailable ? '🟢 IN STOCK' : '🔴 OUT OF STOCK'}
            </span>
            <span class="absolute bottom-2.5 left-2.5 px-2 py-0.5 rounded bg-black/70 backdrop-blur text-[10px] font-bold text-slate-300">
              ${s.category}
            </span>
          </div>

          <div class="p-4 flex-1 flex flex-col justify-between space-y-3">
            <div>
              <h4 class="font-display font-bold text-base text-white tracking-wide leading-snug">${s.name}</h4>
              <p class="text-[11px] text-slate-400 mt-1 line-clamp-2">${s.description || 'Certified authentic gym sports nutrition.'}</p>
            </div>

            <!-- Price & Availability Section -->
            <div class="pt-3 border-t border-gym-border/60">
              ${isOfferActive ? `
                <div class="space-y-1 mb-2">
                  <div class="flex items-center gap-2">
                    <span class="text-xs text-slate-400 line-through font-semibold">${formatInr(origPrice)}</span>
                    <span class="text-[10px] font-bold text-red-400 bg-red-950/80 border border-red-800/80 px-2 py-0.5 rounded-md">🏷️ ${discountText}</span>
                  </div>
                  <div class="flex items-baseline gap-1.5">
                    <span class="font-display font-bold text-2xl text-gym-orange">${formatInr(effectivePrice)}</span>
                  </div>
                </div>
              ` : `
                <div class="mb-2">
                  <span class="font-display font-bold text-2xl text-white">${formatInr(origPrice)}</span>
                </div>
              `}

              ${isAdmin ? `
                <!-- ADMIN ACTIONS ONLY -->
                <div class="flex items-center justify-between pt-2 border-t border-gym-border/40">
                  <span class="text-[10px] text-slate-400 font-mono">
                    Stock: ${stockQty} units
                  </span>
                  <div class="flex gap-1.5">
                    <button onclick="toggleSupplementOffer(${s.id})" title="${s.hasOffer ? 'Disable Offer' : 'Enable Offer'}" class="p-1.5 bg-slate-800 hover:bg-slate-700 text-xs rounded-lg border border-gym-border">
                      ${s.hasOffer ? '🔥' : '🏷️'}
                    </button>
                    <button onclick="toggleSupplementStock(${s.id})" title="Toggle Stock" class="p-1.5 bg-slate-800 hover:bg-slate-700 text-xs rounded-lg border border-gym-border">
                      🔄
                    </button>
                    <button onclick='editSupplementModal(${JSON.stringify(s).replace(/'/g, "&#39;")})' title="Edit" class="p-1.5 bg-slate-800 hover:bg-slate-700 text-xs rounded-lg border border-gym-border">
                      ✏️
                    </button>
                    <button onclick="deleteSupplement(${s.id})" title="Delete" class="p-1.5 bg-red-950/40 hover:bg-gym-crimson text-gym-crimson hover:text-white text-xs rounded-lg border border-red-900/40">
                      🗑️
                    </button>
                  </div>
                </div>
              ` : `
                <!-- USER ACTIONS: BUY NOW -->
                <div class="pt-1">
                  ${isAvailable ? `
                    <button onclick="openBuySupplementModal(${s.id})" class="w-full bg-gradient-to-r from-gym-orange to-red-600 hover:from-gym-orangeHover hover:to-red-700 text-white font-bold py-2.5 px-4 rounded-xl shadow-lg shadow-gym-orangeGlow transition flex items-center justify-center gap-1.5 text-xs">
                      <span>[ BUY NOW ]</span>
                    </button>
                  ` : `
                    <button disabled class="w-full bg-slate-900 border border-red-900/50 text-red-400/80 font-bold py-2.5 px-4 rounded-xl cursor-not-allowed text-xs flex items-center justify-center gap-1">
                      <span>[ BUY NOW disabled ]</span>
                    </button>
                  `}
                </div>
              `}
            </div>
          </div>
        </div>
      `;
    }).join('');
  } catch (e) {
    console.error('Error loading supplements', e);
  }
}

function handleStockStatusChange() {
  const status = document.getElementById('suppStockStatus').value;
  const qtyInput = document.getElementById('suppStockQuantity');
  if (status === 'OUT_OF_STOCK') {
    if (qtyInput) qtyInput.value = '0';
  } else {
    if (qtyInput && parseInt(qtyInput.value) <= 0) qtyInput.value = '25';
  }
}

function openSupplementModal() {
  document.getElementById('supplementForm').reset();
  document.getElementById('supplementEditId').value = '';
  document.getElementById('supplementModalTitle').textContent = 'Add Supplement Product';
  document.getElementById('suppPreviewImg').src = 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=400';
  document.getElementById('suppImage').value = 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=400';
  document.getElementById('suppUploadMsg').textContent = 'Select product image (JPEG, PNG, WEBP)';
  document.getElementById('suppOriginalPrice').value = '';
  document.getElementById('suppStockQuantity').value = '25';
  document.getElementById('suppStockStatus').value = 'AVAILABLE';
  document.getElementById('suppHasOffer').value = 'false';
  document.getElementById('suppOfferType').value = 'PERCENTAGE';
  document.getElementById('suppDiscountValue').value = '';
  document.getElementById('suppOfferStartDate').value = '';
  document.getElementById('suppOfferEndDate').value = '';
  toggleOfferFields();
  document.getElementById('supplementModal').classList.remove('hidden');
}

function editSupplementModal(s) {
  document.getElementById('supplementEditId').value = s.id;
  document.getElementById('suppName').value = s.name;
  document.getElementById('suppCategory').value = s.category;
  
  const origPrice = s.originalPrice > 0 ? s.originalPrice : s.price;
  document.getElementById('suppOriginalPrice').value = origPrice;
  document.getElementById('suppStockStatus').value = s.stockStatus;
  document.getElementById('suppStockQuantity').value = s.stockQuantity != null ? s.stockQuantity : 25;
  
  const hasOffer = s.hasOffer === true || s.hasOffer === 'true';
  document.getElementById('suppHasOffer').value = hasOffer ? 'true' : 'false';
  document.getElementById('suppOfferType').value = s.offerType || 'PERCENTAGE';
  document.getElementById('suppDiscountValue').value = s.discountValue > 0 ? s.discountValue : '';
  document.getElementById('suppOfferStartDate').value = s.offerStartDate || '';
  document.getElementById('suppOfferEndDate').value = s.offerEndDate || '';
  
  document.getElementById('suppDescription').value = s.description || '';
  document.getElementById('suppImage').value = s.imageUrl || 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=400';
  document.getElementById('suppPreviewImg').src = s.imageUrl || 'https://images.unsplash.com/photo-1584017911766-d451b3d0e843?w=400';
  document.getElementById('supplementModalTitle').textContent = 'Edit Supplement Product';
  
  toggleOfferFields();
  recalcOfferPreview();
  document.getElementById('supplementModal').classList.remove('hidden');
}

function closeSupplementModal() {
  document.getElementById('supplementModal').classList.add('hidden');
}

async function handleSupplementSubmit(e) {
  e.preventDefault();
  const id = document.getElementById('supplementEditId').value;
  const originalPrice = parseFloat(document.getElementById('suppOriginalPrice').value) || 0;
  const hasOffer = document.getElementById('suppHasOffer').value === 'true';
  const offerType = document.getElementById('suppOfferType').value;
  const discountValue = hasOffer ? (parseFloat(document.getElementById('suppDiscountValue').value) || 0) : 0;
  const stockQuantity = parseInt(document.getElementById('suppStockQuantity').value) || 0;
  const stockStatus = document.getElementById('suppStockStatus').value;

  // Validation
  if (originalPrice <= 0) {
    showToast('Please enter a valid original price', 'error');
    return;
  }
  if (hasOffer) {
    if (discountValue <= 0) {
      showToast('Please enter a valid discount value', 'error');
      return;
    }
    if (offerType === 'PERCENTAGE' && discountValue >= 100) {
      showToast('Percentage discount must be less than 100%', 'error');
      return;
    }
    if (offerType === 'FIXED' && discountValue >= originalPrice) {
      showToast('Fixed discount cannot be greater than or equal to original price', 'error');
      return;
    }
  }

  const payload = {
    name: document.getElementById('suppName').value.trim(),
    category: document.getElementById('suppCategory').value,
    originalPrice: originalPrice,
    price: originalPrice,
    hasOffer: hasOffer,
    offerType: offerType,
    discountValue: discountValue,
    offerStartDate: document.getElementById('suppOfferStartDate').value || null,
    offerEndDate: document.getElementById('suppOfferEndDate').value || null,
    stockStatus: stockStatus,
    stockQuantity: stockQuantity,
    description: document.getElementById('suppDescription').value.trim(),
    imageUrl: document.getElementById('suppImage').value
  };

  try {
    const url = id ? `/api/supplements/${id}` : '/api/supplements';
    const method = id ? 'PUT' : 'POST';

    const res = await fetch(url, {
      method: method,
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify(payload)
    });

    if (!res.ok) {
      const err = await res.json();
      showToast(err.error || 'Failed to save product', 'error');
      return;
    }

    closeSupplementModal();
    showToast(id ? 'Supplement updated successfully!' : 'Supplement created with offer settings!', 'success');
    loadSupplements();
  } catch (err) {
    showToast('Failed to save supplement product', 'error');
  }
}

async function deleteSupplement(id) {
  if (!confirm('Are you sure you want to remove this supplement product?')) return;
  try {
    const res = await fetch(`/api/supplements/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (res.ok) {
      showToast('Product deleted', 'info');
      loadSupplements();
    }
  } catch (e) {
    showToast('Failed to delete product', 'error');
  }
}

// ================= UPI & QR SECTION =================
let adminSelectedQrBase64 = null;

function previewAdminUpiQr(event) {
  const file = event.target.files && event.target.files[0];
  if (!file) {
    adminSelectedQrBase64 = null;
    const previewContainer = document.getElementById('adminQrFilePreviewContainer');
    if (previewContainer) previewContainer.classList.add('hidden');
    return;
  }
  const reader = new FileReader();
  reader.onload = function(e) {
    adminSelectedQrBase64 = e.target.result;
    const previewContainer = document.getElementById('adminQrFilePreviewContainer');
    const previewImg = document.getElementById('adminQrFilePreviewImg');
    if (previewContainer && previewImg) {
      previewImg.src = adminSelectedQrBase64;
      previewContainer.classList.remove('hidden');
    }
  };
  reader.readAsDataURL(file);
}

function handleAdminQrError(img) {
  if (img) img.style.display = 'none';
  const notice = document.getElementById('adminUpiQrErrorNotice');
  if (notice) notice.classList.remove('hidden');
  const wrapper = document.getElementById('adminQrCardWrapper');
  if (wrapper) wrapper.className = 'my-4 inline-block p-2 bg-transparent rounded-2xl relative';
}

async function loadUpiSection() {
  const qrImg = document.getElementById('displayUpiQrImg');
  const notice = document.getElementById('adminUpiQrErrorNotice');
  const wrapper = document.getElementById('adminQrCardWrapper');

  if (wrapper) wrapper.className = 'my-6 inline-block p-5 bg-white rounded-2xl shadow-2xl relative';
  if (notice) notice.classList.add('hidden');
  if (qrImg) {
    qrImg.style.display = 'block';
    qrImg.src = `/api/upi/qr?t=${Date.now()}`;
  }

  try {
    const res = await fetch('/api/upi');
    if (!res.ok) return;
    const upi = await res.json();
    STATE.currentUpiSetting = upi;

    const displayUpiId = document.getElementById('displayUpiId');
    if (displayUpiId) displayUpiId.textContent = upi.upiId || '8688610528-3@ybl';
    const displayMerchantName = document.getElementById('displayMerchantName');
    if (displayMerchantName) displayMerchantName.textContent = upi.merchantName || 'PowerFitnessKurnool Gym';
    const displayUpiNotes = document.getElementById('displayUpiNotes');
    if (displayUpiNotes) displayUpiNotes.textContent = upi.notes || 'Include your Phone Number or PFK ID in transaction remarks.';

    // If Admin, populate and show config form
    if (STATE.user && STATE.user.role === 'ADMIN') {
      const form = document.getElementById('adminUpiConfigForm');
      if (form) form.classList.remove('hidden');
      const editId = document.getElementById('editUpiId');
      if (editId) editId.value = upi.upiId || '';
      const editName = document.getElementById('editMerchantName');
      if (editName) editName.value = upi.merchantName || '';
      const editNotes = document.getElementById('editUpiNotes');
      if (editNotes) editNotes.value = upi.notes || '';
      
      adminSelectedQrBase64 = null;
      const fileInput = document.getElementById('editUpiQrFile');
      if (fileInput) fileInput.value = '';
      const previewContainer = document.getElementById('adminQrFilePreviewContainer');
      if (previewContainer) previewContainer.classList.add('hidden');
    }
  } catch (e) {
    console.error('Error loading UPI', e);
  }
}

function copyUpiId() {
  const upiId = document.getElementById('displayUpiId').textContent;
  navigator.clipboard.writeText(upiId);
  showToast(`Copied ${upiId} to clipboard`, 'info');
}

async function saveUpiConfig() {
  const upiId = document.getElementById('editUpiId').value.trim();
  const merchantName = document.getElementById('editMerchantName').value.trim();
  const notes = document.getElementById('editUpiNotes').value.trim();

  if (!upiId) {
    showToast('UPI ID is required', 'error');
    return;
  }

  const payload = {
    upiId: upiId,
    merchantName: merchantName || 'PowerFitnessKurnool Gym',
    notes: notes
  };

  if (adminSelectedQrBase64) {
    payload.qrCodeUrl = adminSelectedQrBase64;
  }

  const btn = document.getElementById('saveUpiBtn');
  if (btn) {
    btn.disabled = true;
    btn.innerHTML = '<span>Saving...</span>';
  }

  try {
    const res = await fetch('/api/upi/admin', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify(payload)
    });
    if (res.ok) {
      showToast('✓ UPI QR CODE SAVED', 'success');
      adminSelectedQrBase64 = null;
      const fileInput = document.getElementById('editUpiQrFile');
      if (fileInput) fileInput.value = '';
      const previewContainer = document.getElementById('adminQrFilePreviewContainer');
      if (previewContainer) previewContainer.classList.add('hidden');
      await loadUpiSection();
    } else {
      const err = await res.json();
      showToast(err.error || 'Failed to save UPI config', 'error');
    }
  } catch (e) {
    showToast('Failed to save UPI config', 'error');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '<span>Save UPI Details</span>';
    }
  }
}

// ================= USER DASHBOARD =================
async function loadUserDashboard() {
  try {
    const res = await fetch('/api/dashboard/user', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const d = await res.json();

    document.getElementById('userDashWelcomeName').textContent = `Welcome, ${d.fullName} 💪`;
    document.getElementById('userDashMemberCode').textContent = d.memberCode;
    document.getElementById('userDashPhoto').src = d.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400';

    const statusBadge = document.getElementById('userDashStatusBadge');
    statusBadge.textContent = d.status;
    statusBadge.className = `text-xs font-bold px-2.5 py-1 rounded-md ${getStatusBadgeClass(d.status)}`;

    const cardioBadge = document.getElementById('userDashCardioBadge');
    if (d.hasCardio) cardioBadge.classList.remove('hidden');
    else cardioBadge.classList.add('hidden');

    // Visual Countdown
    document.getElementById('userDashCountdown').textContent = d.countdownDisplay;
    document.getElementById('userDashExpiryDate').textContent = `Valid Until: ${d.expiryDate}`;

    // Progress Bar
    document.getElementById('userDashProgressText').textContent = `${d.daysCompleted} of ${d.totalDays} days completed`;
    document.getElementById('userDashProgressPercent').textContent = `${d.progressPercent}%`;
    document.getElementById('userDashProgressBar').style.width = `${d.progressPercent}%`;

    // Plan info
    document.getElementById('userDashPlan').textContent = d.subscriptionPlan;
    document.getElementById('userDashCategory').textContent = d.trainingCategory;
    document.getElementById('userDashBatch').textContent = d.batch;
    document.getElementById('userDashPhone').textContent = d.phoneNumber;

  } catch (e) {
    console.error('Error loading user dashboard', e);
  }
}

async function loadUserProfileCard() {
  try {
    const res = await fetch('/api/user/profile', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const m = await res.json();

    // Digital Membership Card
    if (document.getElementById('cardPhoto')) document.getElementById('cardPhoto').src = m.photoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=400';
    if (document.getElementById('cardFullName')) document.getElementById('cardFullName').textContent = m.fullName;
    if (document.getElementById('cardMemberCode')) document.getElementById('cardMemberCode').textContent = `ID: ${m.memberCode}`;
    if (document.getElementById('cardPhone')) document.getElementById('cardPhone').textContent = `Phone: ${m.phoneNumber}`;
    if (document.getElementById('cardPlan')) document.getElementById('cardPlan').textContent = m.subscriptionPlan;
    if (document.getElementById('cardBatch')) document.getElementById('cardBatch').textContent = m.batch;
    if (document.getElementById('cardCategory')) document.getElementById('cardCategory').textContent = m.trainingCategory;
    if (document.getElementById('cardStartDate')) document.getElementById('cardStartDate').textContent = m.startDate;
    if (document.getElementById('cardExpiryDate')) document.getElementById('cardExpiryDate').textContent = m.expiryDate;
    if (document.getElementById('cardCardioStatus')) document.getElementById('cardCardioStatus').textContent = m.hasCardio ? 'Cardio Included' : 'No Cardio';

    const b = document.getElementById('cardStatusBadge');
    if (b) {
      b.textContent = m.status;
      b.className = `text-[11px] font-bold px-2.5 py-1 rounded-md ${getStatusBadgeClass(m.status)}`;
    }

    // Read-only Member Information Cards
    if (document.getElementById('memberProfileName')) document.getElementById('memberProfileName').textContent = m.fullName || '-';
    if (document.getElementById('memberProfileCode')) document.getElementById('memberProfileCode').textContent = m.memberCode || '-';
    if (document.getElementById('memberProfilePhone')) document.getElementById('memberProfilePhone').textContent = m.phoneNumber || '-';
    if (document.getElementById('memberProfileAdmission')) document.getElementById('memberProfileAdmission').textContent = m.startDate || '-';
    if (document.getElementById('memberProfileBatch')) document.getElementById('memberProfileBatch').textContent = m.batch || '-';
    if (document.getElementById('memberProfilePlanStatus')) document.getElementById('memberProfilePlanStatus').textContent = `${m.subscriptionPlan || 'Membership'} (${m.status || 'Active'})`;

    // Clear previous password change message
    const msgEl = document.getElementById('memberPasswordMsg');
    if (msgEl) {
      msgEl.className = 'hidden p-3 rounded-xl text-xs font-semibold';
      msgEl.textContent = '';
    }
  } catch (e) {
    console.error('Error loading profile card', e);
  }
}

// ================= ACCOUNT & CREDENTIAL MANAGEMENT =================
function handleProfileAreaClick() {
  if (STATE.user && STATE.user.role === 'ADMIN') {
    openAdminAccountModal();
  } else {
    navigateTo('userProfile');
  }
}

// --- Member Password Change ---
async function updateMemberPassword(e) {
  if (e) e.preventDefault();
  const currentPassword = document.getElementById('memberCurrentPassword')?.value || '';
  const newPassword = document.getElementById('memberNewPassword')?.value || '';
  const confirmPassword = document.getElementById('memberConfirmPassword')?.value || '';
  const msgEl = document.getElementById('memberPasswordMsg');

  if (!currentPassword) {
    showInlineMsg(msgEl, 'Current password is required', 'error');
    return;
  }
  if (newPassword.length < 6) {
    showInlineMsg(msgEl, 'New password must be at least 6 characters long', 'error');
    return;
  }
  if (newPassword !== confirmPassword) {
    showInlineMsg(msgEl, 'New passwords do not match', 'error');
    return;
  }

  try {
    const res = await fetch('/api/auth/member/password', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({ currentPassword, newPassword, confirmPassword })
    });
    const data = await res.json();
    if (res.ok) {
      showInlineMsg(msgEl, '✓ Password changed successfully', 'success');
      showToast('Password changed successfully!', 'success');
      document.getElementById('memberCurrentPassword').value = '';
      document.getElementById('memberNewPassword').value = '';
      document.getElementById('memberConfirmPassword').value = '';
    } else {
      showInlineMsg(msgEl, data.error || 'Failed to change password', 'error');
    }
  } catch (err) {
    showInlineMsg(msgEl, 'Network error. Please try again.', 'error');
  }
}

// --- Admin Account Modal ---
function openAdminAccountModal() {
  const modal = document.getElementById('adminAccountModal');
  if (!modal) return;
  const currentU = (STATE.user && STATE.user.username) ? STATE.user.username : 'admin';
  const displayEl = document.getElementById('displayCurrentAdminUsername');
  if (displayEl) displayEl.textContent = currentU;
  const newU = document.getElementById('adminNewUsername');
  if (newU) newU.value = '';
  const curP = document.getElementById('adminCurrentPassword');
  if (curP) curP.value = '';
  const newP = document.getElementById('adminNewPassword');
  if (newP) newP.value = '';
  const confP = document.getElementById('adminConfirmPassword');
  if (confP) confP.value = '';
  const msgEl = document.getElementById('adminAccountMsg');
  if (msgEl) {
    msgEl.className = 'hidden p-3.5 rounded-xl text-xs font-semibold';
    msgEl.textContent = '';
  }
  modal.classList.remove('hidden');
}

function closeAdminAccountModal() {
  const modal = document.getElementById('adminAccountModal');
  if (modal) modal.classList.add('hidden');
}

async function updateAdminUsername() {
  const newUsername = document.getElementById('adminNewUsername')?.value.trim() || '';
  const msgEl = document.getElementById('adminAccountMsg');

  if (!newUsername) {
    showInlineMsg(msgEl, 'New username is required', 'error');
    return;
  }
  if (newUsername.length < 3) {
    showInlineMsg(msgEl, 'New username must be at least 3 characters long', 'error');
    return;
  }

  try {
    const res = await fetch('/api/auth/admin/username', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({ newUsername })
    });
    const data = await res.json();
    if (res.ok) {
      showInlineMsg(msgEl, '✓ Username updated successfully', 'success');
      showToast('✓ Username updated successfully!', 'success');
      if (STATE.user) STATE.user.username = newUsername;
      try { localStorage.setItem('pf_user', JSON.stringify(STATE.user)); } catch (e) {}
      const displayEl = document.getElementById('displayCurrentAdminUsername');
      if (displayEl) displayEl.textContent = newUsername;
      const headerName = document.getElementById('headerAdminName');
      if (headerName) headerName.textContent = newUsername;
      const sidebarName = document.getElementById('sidebarUserName');
      if (sidebarName) sidebarName.textContent = newUsername;
      document.getElementById('adminNewUsername').value = '';
    } else {
      showInlineMsg(msgEl, data.error || 'Failed to update username', 'error');
    }
  } catch (err) {
    showInlineMsg(msgEl, 'Network error. Please try again.', 'error');
  }
}

async function updateAdminPassword() {
  const currentPassword = document.getElementById('adminCurrentPassword')?.value || '';
  const newPassword = document.getElementById('adminNewPassword')?.value || '';
  const confirmPassword = document.getElementById('adminConfirmPassword')?.value || '';
  const msgEl = document.getElementById('adminAccountMsg');

  if (!currentPassword) {
    showInlineMsg(msgEl, 'Current password is required', 'error');
    return;
  }
  if (newPassword.length < 6) {
    showInlineMsg(msgEl, 'New password must be at least 6 characters long', 'error');
    return;
  }
  if (newPassword !== confirmPassword) {
    showInlineMsg(msgEl, 'New passwords do not match', 'error');
    return;
  }

  try {
    const res = await fetch('/api/auth/admin/password', {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({ currentPassword, newPassword, confirmPassword })
    });
    const data = await res.json();
    if (res.ok) {
      showInlineMsg(msgEl, '✓ Password changed successfully', 'success');
      showToast('✓ Password changed successfully!', 'success');
      document.getElementById('adminCurrentPassword').value = '';
      document.getElementById('adminNewPassword').value = '';
      document.getElementById('adminConfirmPassword').value = '';
    } else {
      showInlineMsg(msgEl, data.error || 'Failed to change password', 'error');
    }
  } catch (err) {
    showInlineMsg(msgEl, 'Network error. Please try again.', 'error');
  }
}

function showInlineMsg(el, text, type) {
  if (!el) return;
  el.classList.remove('hidden');
  if (type === 'success') {
    el.className = 'p-3.5 rounded-xl text-xs font-semibold bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 block';
  } else {
    el.className = 'p-3.5 rounded-xl text-xs font-semibold bg-red-500/10 border border-red-500/30 text-red-400 block';
  }
  el.textContent = text;
}

// ================= CALORIES & ANDHRA MEALS =================
async function loadCaloriesView() {
  await loadUserGoal();
  await loadTrackerData();
  await loadFoodDatabase();
}

async function loadUserGoal() {
  try {
    const res = await fetch('/api/nutrition/goal', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const goal = await res.json();
    STATE.activeGoal = goal.goalType;
    document.getElementById('displayActiveGoal').textContent = goal.goalType;
  } catch (e) {}
}

async function selectFitnessGoal(goalType, category) {
  try {
    const res = await fetch('/api/nutrition/goal', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({ category, goalType })
    });
    if (res.ok) {
      const g = await res.json();
      STATE.activeGoal = g.goalType;
      document.getElementById('displayActiveGoal').textContent = g.goalType;
      showToast(`Goal updated to ${g.goalType}! Targets adjusted.`, 'success');
      loadTrackerData();
    }
  } catch (e) {
    showToast('Failed to update goal', 'error');
  }
}

async function loadTrackerData() {
  const dateInput = document.getElementById('trackerDateInput');
  const date = dateInput ? dateInput.value : new Date().toISOString().split('T')[0];

  try {
    const res = await fetch(`/api/nutrition/tracker?date=${date}`, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const d = await res.json();

    // Update Progress Bars
    const calPct = Math.min(100, Math.round((d.totalCalories / d.targetCalories) * 100));
    document.getElementById('calText').textContent = `${d.totalCalories} / ${d.targetCalories} kcal`;
    document.getElementById('calProgress').style.width = `${calPct}%`;
    document.getElementById('calRemaining').textContent = `Remaining: ${d.remainingCalories} kcal`;

    const pPct = Math.min(100, Math.round((d.totalProtein / d.targetProtein) * 100));
    document.getElementById('proteinText').textContent = `${d.totalProtein} / ${d.targetProtein}g`;
    document.getElementById('proteinProgress').style.width = `${pPct}%`;
    document.getElementById('proteinRemaining').textContent = `Remaining: ${d.remainingProtein}g`;

    const cPct = Math.min(100, Math.round((d.totalCarbs / d.targetCarbs) * 100));
    document.getElementById('carbsText').textContent = `${d.totalCarbs} / ${d.targetCarbs}g`;
    document.getElementById('carbsProgress').style.width = `${cPct}%`;
    document.getElementById('carbsRemaining').textContent = `Remaining: ${d.remainingCarbs}g`;

    const fPct = Math.min(100, Math.round((d.totalFat / d.targetFat) * 100));
    document.getElementById('fatText').textContent = `${d.totalFat} / ${d.targetFat}g`;
    document.getElementById('fatProgress').style.width = `${fPct}%`;
    document.getElementById('fatRemaining').textContent = `Remaining: ${d.remainingFat}g`;

    // Vitamins & Minerals Highlights
    document.getElementById('vitaminsText').textContent = d.vitamins || 'Vitamin B Complex, C, D';
    document.getElementById('mineralsText').textContent = d.minerals || 'Iron, Calcium, Potassium, Zinc';

    // Render Meals by Category
    renderTrackerCategory('Breakfast', d.sections.Breakfast || []);
    renderTrackerCategory('Lunch', d.sections.Lunch || []);
    renderTrackerCategory('Snacks', d.sections.Snacks || []);
    renderTrackerCategory('Dinner', d.sections.Dinner || []);

  } catch (e) {
    console.error('Error loading tracker', e);
  }
}

function renderTrackerCategory(category, items) {
  const listEl = document.getElementById(`tracker${category}List`);
  const totalCalsEl = document.getElementById(`${category.toLowerCase()}TotalCals`);

  let catCals = 0;
  if (!items || items.length === 0) {
    listEl.innerHTML = `<p class="text-slate-500 text-xs text-center py-3">No ${category.toLowerCase()} meals selected</p>`;
    if (totalCalsEl) totalCalsEl.textContent = '0 kcal';
    return;
  }

  listEl.innerHTML = items.map(item => {
    catCals += item.calories;
    const food = item.mealFood;
    return `
      <div class="flex items-center justify-between p-2 rounded-xl bg-slate-900/80 border border-gym-border text-xs">
        <div class="flex items-center gap-2.5">
          <img src="${food.imageUrl || 'https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=100'}" class="w-9 h-9 rounded-lg object-cover">
          <div>
            <span class="font-bold text-white block">${food.name}</span>
            <span class="text-[10px] text-slate-400 font-mono">${item.quantity}x (${food.servingSize}) • ${Math.round(item.protein)}g P • ${Math.round(item.carbs)}g C</span>
          </div>
        </div>
        <div class="flex items-center gap-2">
          <span class="font-display font-bold text-gym-orange">${Math.round(item.calories)} kcal</span>
          <button onclick="removeMealItem(${item.id})" class="text-slate-500 hover:text-gym-crimson p-1" title="Remove">✕</button>
        </div>
      </div>
    `;
  }).join('');

  if (totalCalsEl) totalCalsEl.textContent = `${Math.round(catCals)} kcal`;
}

async function removeMealItem(id) {
  try {
    const res = await fetch(`/api/nutrition/tracker/${id}`, {
      method: 'DELETE',
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (res.ok) {
      showToast('Item removed', 'info');
      loadTrackerData();
    }
  } catch (e) {
    showToast('Failed to remove item', 'error');
  }
}

// Food Database
function filterFoodCategory(cat) {
  STATE.foodCategoryFilter = cat;
  ['All', 'Breakfast', 'Lunch', 'Snacks', 'Dinner'].forEach(c => {
    const btn = document.getElementById(`foodTab${c}`);
    if (btn) {
      if (c === cat) btn.className = 'px-3 py-1.5 rounded-lg text-xs font-bold uppercase bg-gym-orange text-white';
      else btn.className = 'px-3 py-1.5 rounded-lg text-xs font-bold uppercase bg-slate-900 text-slate-400 hover:text-white';
    }
  });
  loadFoodDatabase();
}

function toggleAndhraSpecialFilter() {
  STATE.andhraOnly = !STATE.andhraOnly;
  const btn = document.getElementById('foodTabAndhra');
  if (STATE.andhraOnly) {
    btn.className = 'px-3 py-1.5 rounded-lg text-xs font-bold uppercase bg-amber-500 text-black shadow-md';
  } else {
    btn.className = 'px-3 py-1.5 rounded-lg text-xs font-bold uppercase bg-slate-900 text-amber-400 border border-amber-500/30 hover:bg-amber-500/10';
  }
  loadFoodDatabase();
}

async function loadFoodDatabase() {
  const params = new URLSearchParams();
  if (STATE.foodCategoryFilter !== 'All') params.append('category', STATE.foodCategoryFilter);
  if (STATE.andhraOnly) params.append('andhraSpecial', 'true');

  try {
    const res = await fetch(`/api/nutrition/foods?${params.toString()}`);
    if (!res.ok) return;
    const foods = await res.json();

    const grid = document.getElementById('foodDatabaseGrid');
    if (foods.length === 0) {
      grid.innerHTML = '<p class="col-span-3 text-center py-8 text-slate-500">No foods in this category.</p>';
      return;
    }

    grid.innerHTML = foods.map(f => `
      <div class="bg-slate-900/80 border border-gym-border rounded-2xl overflow-hidden flex flex-col group hover:border-gym-orange/40 transition">
        <div class="h-32 bg-slate-950 relative overflow-hidden">
          <img src="${f.imageUrl || 'https://images.unsplash.com/photo-1546833999-b9f581a1996d?w=400'}" class="w-full h-full object-cover group-hover:scale-105 transition duration-300">
          ${f.andhraSpecial ? '<span class="absolute top-2 left-2 bg-amber-500 text-black text-[9px] font-bold uppercase px-2 py-0.5 rounded shadow">🌶️ Andhra Special</span>' : ''}
          <span class="absolute top-2 right-2 bg-black/70 text-slate-300 text-[10px] font-bold px-2 py-0.5 rounded backdrop-blur">${f.mealCategory}</span>
        </div>

        <div class="p-3.5 flex-1 flex flex-col justify-between space-y-2 text-xs">
          <div>
            <div class="flex items-center justify-between">
              <h5 class="font-bold text-white">${f.name}</h5>
              <span class="font-display font-bold text-gym-orange text-sm">${Math.round(f.calories)} kcal</span>
            </div>
            <p class="text-[10px] text-slate-400">Serving: ${f.servingSize}</p>
          </div>

          <!-- Macros Badges -->
          <div class="grid grid-cols-3 gap-1 text-[10px] text-center font-bold">
            <div class="bg-emerald-950/40 text-gym-emerald p-1 rounded border border-emerald-900/40">
              <span>${f.protein}g</span> P
            </div>
            <div class="bg-amber-950/40 text-amber-400 p-1 rounded border border-amber-900/40">
              <span>${f.carbs}g</span> C
            </div>
            <div class="bg-cyan-950/40 text-cyan-400 p-1 rounded border border-cyan-900/40">
              <span>${f.fat}g</span> F
            </div>
          </div>

          <!-- Micronutrients -->
          ${f.vitamins ? `<p class="text-[9px] text-slate-400 truncate"><strong>Vitamins:</strong> ${f.vitamins}</p>` : ''}

          <!-- Add Button -->
          <button onclick="addFoodToTracker(${f.id}, '${f.mealCategory}')" class="w-full mt-1 py-1.5 rounded-lg bg-slate-800 hover:bg-gym-orange text-white text-[11px] font-bold uppercase tracking-wider transition flex items-center justify-center gap-1">
            <span>+ Add to ${f.mealCategory}</span>
          </button>
        </div>
      </div>
    `).join('');
  } catch (e) {
    console.error('Error loading food database', e);
  }
}

async function addFoodToTracker(foodId, defaultCategory) {
  const dateInput = document.getElementById('trackerDateInput');
  const date = dateInput ? dateInput.value : new Date().toISOString().split('T')[0];

  try {
    const res = await fetch('/api/nutrition/tracker', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({
        foodId: foodId,
        mealCategory: defaultCategory,
        quantity: 1,
        logDate: date
      })
    });
    if (res.ok) {
      showToast('Meal logged successfully', 'success');
      loadTrackerData();
    }
  } catch (e) {
    showToast('Failed to log meal', 'error');
  }
}

// ================= USER PAYMENTS & UPI =================
async function loadUserPaymentsView() {
  try {
    // 1. Load member details & due fee
    const profileRes = await fetch('/api/user/profile', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (profileRes.ok) {
      const m = await profileRes.json();
      document.getElementById('userPaymentMemberName').textContent = m.fullName;
      document.getElementById('userPaymentDueAmount').textContent = formatInr(m.totalFee);

      // Generate member-specific UPI QR Code
      const qrBox = document.getElementById('userUpiQrContainer');
      qrBox.innerHTML = '';
      const upiUri = `upi://pay?pa=powerfitnesskurnool@okaxis&pn=PowerFitnessKurnool&am=${m.totalFee}&cu=INR`;
      new QRCode(qrBox, {
        text: upiUri,
        width: 160,
        height: 160,
        colorDark: '#000000',
        colorLight: '#ffffff'
      });
    }

    // 2. Load personal payments
    const payRes = await fetch('/api/user/payments', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (payRes.ok) {
      const payments = await payRes.json();
      const tbody = document.getElementById('userPaymentsTableBody');
      if (payments.length === 0) {
        tbody.innerHTML = '<tr><td colspan="6" class="p-4 text-center text-slate-500">No payment receipts yet.</td></tr>';
        return;
      }

      tbody.innerHTML = payments.map(p => `
        <tr class="hover:bg-slate-800/40">
          <td class="p-3 font-mono text-gym-orange">${p.receiptNumber}</td>
          <td class="p-3 font-semibold text-white">${p.subscriptionPlan}</td>
          <td class="p-3 font-display font-bold text-white">${formatInr(p.amount)}</td>
          <td class="p-3">${p.paymentMethod}</td>
          <td class="p-3"><span class="px-2 py-0.5 rounded text-[10px] font-bold ${getPaymentStatusBadge(p.paymentStatus)}">${p.paymentStatus}</span></td>
          <td class="p-3 text-slate-400">${p.paymentDate}</td>
        </tr>
      `).join('');
    }
  } catch (e) {
    console.error('Error loading user payments', e);
  }
}

async function submitUserUtr() {
  const utr = document.getElementById('userUtrInput').value.trim();
  if (!utr) {
    showToast('Please enter your UTR / Transaction Reference', 'error');
    return;
  }

  try {
    const res = await fetch('/api/user/payments/submit-utr', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({ utr })
    });
    if (res.ok) {
      showToast('Payment proof submitted! Admin will verify shortly.', 'success');
      document.getElementById('userUtrInput').value = '';
      loadUserPaymentsView();
    } else {
      showToast('Failed to submit UTR', 'error');
    }
  } catch (e) {
    showToast('Network error', 'error');
  }
}

// ================= REPORTS =================
async function loadReports() {
  try {
    const res = await fetch('/api/admin/reports', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const d = await res.json();

    document.getElementById('reportTotalMembers').textContent = d.totalMembers ?? 0;
    document.getElementById('reportActiveMembers').textContent = d.activeMembers ?? 0;
    document.getElementById('reportExpiredMembers').textContent = d.expiredMembers ?? 0;
    document.getElementById('reportTotalRev').textContent = formatInr(d.totalRevenue);

    // Chart 1: Plan Popularity (Bar)
    const ctx1 = document.getElementById('reportPlanChart');
    if (ctx1) {
      if (STATE.charts.reportPlan) STATE.charts.reportPlan.destroy();
      const plans = d.subscriptionDistribution || {};
      STATE.charts.reportPlan = new Chart(ctx1, {
        type: 'bar',
        data: {
          labels: Object.keys(plans),
          datasets: [{
            label: 'Athletes',
            data: Object.values(plans),
            backgroundColor: '#ff5722',
            borderRadius: 6
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          scales: {
            y: { grid: { color: '#1f293d' }, ticks: { color: '#94a3b8' } },
            x: { grid: { display: false }, ticks: { color: '#94a3b8' } }
          },
          plugins: { legend: { display: false } }
        }
      });
    }

    // Chart 2: Payment Methods (Doughnut)
    const ctx2 = document.getElementById('reportMethodChart');
    if (ctx2) {
      if (STATE.charts.reportMethod) STATE.charts.reportMethod.destroy();
      const methods = d.paymentMethodDistribution || {};
      STATE.charts.reportMethod = new Chart(ctx2, {
        type: 'doughnut',
        data: {
          labels: Object.keys(methods),
          datasets: [{
            data: Object.values(methods),
            backgroundColor: ['#06b6d4', '#10b981', '#f59e0b'],
            borderWidth: 2,
            borderColor: '#101622'
          }]
        },
        options: {
          responsive: true,
          maintainAspectRatio: false,
          plugins: {
            legend: { position: 'bottom', labels: { color: '#94a3b8' } }
          }
        }
      });
    }

  } catch (e) {
    console.error('Error loading reports', e);
  }
}

async function downloadReport(type, format) {
  try {
    showToast(`Generating ${type.toUpperCase()} ${format.toUpperCase()} report...`, 'info');
    const endpoint = `/api/admin/reports/export/${type}.${format}`;
    const res = await fetch(endpoint, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });

    if (!res.ok) {
      showToast('Report generation failed.', 'error');
      return;
    }

    const blob = await res.blob();
    const filename = `PowerFitnessKurnool_${type.charAt(0).toUpperCase() + type.slice(1)}.${format}`;
    const blobUrl = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = blobUrl;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(blobUrl);
    showToast('Report downloaded successfully.', 'success');
  } catch (err) {
    console.error('Report download error:', err);
    showToast('Report generation failed.', 'error');
  }
}

function exportMembersCsv() {
  downloadReport('members', 'csv');
}

function exportPaymentsCsv() {
  downloadReport('payments', 'csv');
}

// ================= NOTIFICATIONS =================
async function loadNotifications() {
  try {
    const res = await fetch('/api/notifications', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const notifs = await res.json();

    const badge = document.getElementById('notifBadge');
    const unread = notifs.filter(n => !n.read).length;
    if (unread > 0) {
      badge.textContent = unread;
      badge.classList.remove('hidden');
    } else {
      badge.classList.add('hidden');
    }

    document.getElementById('notifCountText').textContent = `${unread} unread`;

    const list = document.getElementById('notifList');
    if (notifs.length === 0) {
      list.innerHTML = '<p class="text-slate-500 text-center py-4 text-xs">No notifications</p>';
      return;
    }

    list.innerHTML = notifs.map(n => `
      <div class="p-2 rounded-xl ${n.read ? 'bg-slate-900/40 text-slate-400' : 'bg-slate-800 text-white font-medium'} border border-gym-border">
        <p class="font-bold text-xs">${n.title}</p>
        <p class="text-[11px] text-slate-400 mt-0.5">${n.message}</p>
        <span class="text-[9px] text-slate-500 mt-1 block">${new Date(n.createdAt).toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}</span>
      </div>
    `).join('');
  } catch (e) {}
}

function toggleNotificationsDropdown() {
  const dd = document.getElementById('notifDropdown');
  dd.classList.toggle('hidden');
}

// ================= HELPERS & FORMATTERS =================
function formatInr(val) {
  if (val == null) return '₹0';
  return '₹' + Number(val).toLocaleString('en-IN');
}

function getStatusBadgeClass(status) {
  switch (status) {
    case 'ACTIVE': return 'bg-emerald-950/80 text-gym-emerald border border-gym-emerald/30';
    case 'EXPIRING_SOON': return 'bg-amber-950/80 text-gym-gold border border-gym-gold/30';
    case 'EXPIRED': return 'bg-red-950/80 text-gym-crimson border border-gym-crimson/30';
    default: return 'bg-slate-800 text-slate-300';
  }
}

function getPaymentStatusBadge(status) {
  switch (status) {
    case 'Paid': return 'bg-emerald-950/60 text-gym-emerald border border-gym-emerald/30';
    case 'Pending': return 'bg-amber-950/60 text-gym-gold border border-gym-gold/30';
    case 'Failed': return 'bg-red-950/60 text-gym-crimson border border-gym-crimson/30';
    default: return 'bg-slate-800 text-slate-400';
  }
}

function getPaymentStatusColor(status) {
  switch (status) {
    case 'Paid': return 'text-gym-emerald';
    case 'Pending': return 'text-gym-gold';
    case 'Failed': return 'text-gym-crimson';
    default: return 'text-slate-300';
  }
}

function getDaysRemainingColor(days) {
  if (days < 0) return 'text-gym-crimson';
  if (days <= 7) return 'text-gym-gold';
  return 'text-gym-emerald';
}

function escapeHtml(str) {
  if (!str) return '';
  return str.replace(/'/g, "\\'");
}

function showToast(msg, type = 'info') {
  const container = document.getElementById('toastContainer');
  const toast = document.createElement('div');
  const bg = type === 'success' ? 'bg-emerald-600' : type === 'error' ? 'bg-red-600' : 'bg-gym-orange';

  toast.className = `${bg} text-white px-4 py-2.5 rounded-xl shadow-2xl text-xs font-bold flex items-center gap-2 transform transition-all duration-300 pointer-events-auto`;
  toast.innerHTML = `<span>${type === 'success' ? '✓' : type === 'error' ? '✕' : 'ℹ'}</span><span>${msg}</span>`;

  container.appendChild(toast);
  setTimeout(() => {
    toast.classList.add('opacity-0', 'translate-y-2');
    setTimeout(() => toast.remove(), 300);
  }, 3500);
}


// =========================================================================
// ============= SUPPLEMENT STORE: BUY, ORDERS & COLLECTION SYSTEM ==========
// =========================================================================

// --- 1. USER: BUY SUPPLEMENT 2-STEP WORKFLOW ---
let activeBuySupplement = null;
let buyUpiQrCodeInstance = null;

function openBuySupplementModal(suppId) {
  if (!STATE.supplementsCache) {
    showToast('Please wait for supplements to load', 'info');
    return;
  }
  const s = STATE.supplementsCache.find(item => item.id == suppId);
  if (!s) return;

  activeBuySupplement = s;
  const origPrice = s.originalPrice > 0 ? s.originalPrice : s.price;
  const isOfferActive = s.hasOffer && s.offerPrice > 0 && (s.price < origPrice || s.offerPrice < origPrice);
  const effectivePrice = isOfferActive ? s.offerPrice : origPrice;
  const stock = s.stockQuantity != null ? s.stockQuantity : 0;

  if (stock <= 0) {
    showToast('This item is currently out of stock', 'error');
    return;
  }

  // Reset to Step 1
  document.getElementById('buyStep1Confirm').classList.remove('hidden');
  document.getElementById('buyStep2Payment').classList.add('hidden');

  document.getElementById('buySuppId').value = s.id;
  document.getElementById('buySuppImage').src = s.imageUrl || 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=400';
  document.getElementById('buySuppName').textContent = s.name;
  document.getElementById('buySuppCategory').textContent = s.category;
  document.getElementById('buySuppStockCount').textContent = stock;
  document.getElementById('buySuppQuantity').value = 1;
  document.getElementById('buySuppQuantity').max = stock;

  const origPriceEl = document.getElementById('buySuppOriginalPrice');
  const discountBadge = document.getElementById('buySuppDiscountBadge');
  if (isOfferActive) {
    origPriceEl.textContent = formatInr(origPrice);
    origPriceEl.classList.remove('hidden');
    discountBadge.textContent = s.offerType === 'FIXED' 
      ? `₹${Math.round(s.discountValue).toLocaleString('en-IN')} OFF` 
      : `${Math.round(s.discountValue)}% OFF`;
    discountBadge.classList.remove('hidden');
  } else {
    origPriceEl.classList.add('hidden');
    discountBadge.classList.add('hidden');
  }

  document.getElementById('buySuppUnitPrice').textContent = formatInr(effectivePrice);
  document.getElementById('buyUpiRef').value = '';

  recalculateBuyTotal();
  document.getElementById('buySupplementModal').classList.remove('hidden');
}

function closeBuySupplementModal() {
  document.getElementById('buySupplementModal').classList.add('hidden');
  activeBuySupplement = null;
}

function adjustBuyQuantity(delta) {
  const input = document.getElementById('buySuppQuantity');
  let val = parseInt(input.value) || 1;
  val += delta;
  const max = activeBuySupplement && activeBuySupplement.stockQuantity ? activeBuySupplement.stockQuantity : 99;
  if (val < 1) val = 1;
  if (val > max) val = max;
  input.value = val;
  recalculateBuyTotal();
}

function onBuyQuantityChange() {
  const input = document.getElementById('buySuppQuantity');
  let val = parseInt(input.value) || 1;
  const max = activeBuySupplement && activeBuySupplement.stockQuantity ? activeBuySupplement.stockQuantity : 99;
  if (val < 1) val = 1;
  if (val > max) val = max;
  input.value = val;
  recalculateBuyTotal();
}

function recalculateBuyTotal() {
  if (!activeBuySupplement) return;
  const s = activeBuySupplement;
  const origPrice = s.originalPrice > 0 ? s.originalPrice : s.price;
  const isOfferActive = s.hasOffer && s.offerPrice > 0 && (s.price < origPrice || s.offerPrice < origPrice);
  const effectivePrice = isOfferActive ? s.offerPrice : origPrice;

  const qty = parseInt(document.getElementById('buySuppQuantity').value) || 1;
  const total = Math.round(effectivePrice * qty);

  document.getElementById('buySuppTotalAmount').textContent = formatInr(total);
}

async function proceedToPayment() {
  if (!activeBuySupplement) return;
  const s = activeBuySupplement;
  const origPrice = s.originalPrice > 0 ? s.originalPrice : s.price;
  const isOfferActive = s.hasOffer && s.offerPrice > 0 && (s.price < origPrice || s.offerPrice < origPrice);
  const effectivePrice = isOfferActive ? s.offerPrice : origPrice;

  const qty = parseInt(document.getElementById('buySuppQuantity').value) || 1;
  const total = Math.round(effectivePrice * qty);

  document.getElementById('upiOrderProductName').textContent = s.name;
  document.getElementById('upiOrderQuantity').textContent = `Quantity: ${qty}`;
  document.getElementById('upiOrderAmount').textContent = formatInr(total);

  // Retrieve current persistent QR code and UPI ID saved by Admin
  const qrImg = document.getElementById('buyUpiQrImage');
  const errorNotice = document.getElementById('buyUpiQrErrorNotice');
  const upiIdDisplay = document.getElementById('buyDisplayUpiId');
  const wrapper = document.getElementById('buyQrCardWrapper');

  if (wrapper) wrapper.className = 'p-4 bg-white rounded-2xl inline-block shadow-2xl my-1 relative max-w-[240px] mx-auto transition-all';
  if (errorNotice) errorNotice.classList.add('hidden');
  if (qrImg) {
    qrImg.style.display = 'block';
    qrImg.src = `/api/upi/qr?t=${Date.now()}`;
  }

  try {
    const res = await fetch('/api/upi');
    if (res.ok) {
      const upi = await res.json();
      if (upiIdDisplay) {
        upiIdDisplay.textContent = upi.upiId || '8688610528-3@ybl';
      }
    }
  } catch (err) {
    console.error('Error fetching UPI settings:', err);
  }

  // Switch to Step 2
  document.getElementById('buyStep1Confirm').classList.add('hidden');
  document.getElementById('buyStep2Payment').classList.remove('hidden');
}

function handleBuyQrError(img) {
  if (img) img.style.display = 'none';
  const notice = document.getElementById('buyUpiQrErrorNotice');
  if (notice) notice.classList.remove('hidden');
  const wrapper = document.getElementById('buyQrCardWrapper');
  if (wrapper) wrapper.className = 'p-3 bg-transparent rounded-2xl inline-block my-1 relative max-w-[260px] mx-auto transition-all';
}

function copyBuyUpiId() {
  const el = document.getElementById('buyDisplayUpiId');
  if (el) {
    navigator.clipboard.writeText(el.textContent.trim());
    showToast(`Copied ${el.textContent.trim()} to clipboard`, 'info');
  }
}

function backToOrderSummary() {
  document.getElementById('buyStep2Payment').classList.add('hidden');
  document.getElementById('buyStep1Confirm').classList.remove('hidden');
}

async function submitSupplementOrder() {
  if (!activeBuySupplement) return;
  const suppId = activeBuySupplement.id;
  const qty = parseInt(document.getElementById('buySuppQuantity').value) || 1;
  const upiRef = document.getElementById('buyUpiRef').value.trim();
  const confirmBtn = document.getElementById('confirmOrderBtn');

  confirmBtn.disabled = true;
  confirmBtn.innerHTML = '<span>Creating Order...</span>';

  try {
    const res = await fetch('/api/orders/buy', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({
        supplementId: suppId,
        quantity: qty,
        paymentMethod: 'UPI',
        upiTransactionRef: upiRef
      })
    });

    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Failed to place order', 'error');
      confirmBtn.disabled = false;
      confirmBtn.innerHTML = '<span>✓ Confirm Order</span>';
      return;
    }

    closeBuySupplementModal();
    showToast(`Order ${data.orderNumber} created! Total: ₹${Math.round(data.totalAmount)}. Payment Status: PENDING.`, 'success');
    navigateTo('userOrders');
  } catch (e) {
    showToast('Failed to place order. Please try again.', 'error');
  } finally {
    if (confirmBtn) {
      confirmBtn.disabled = false;
      confirmBtn.innerHTML = '<span>✓ Confirm Order</span>';
    }
  }
}
// --- 2. ADMIN: SUPPLEMENT ORDERS DASHBOARD ---
let orderSearchDebounceTimer = null;

function debounceOrderSearch() {
  clearTimeout(orderSearchDebounceTimer);
  orderSearchDebounceTimer = setTimeout(() => {
    loadSupplementOrders();
  }, 300);
}

async function loadSupplementOrders() {
  try {
    const filter = document.getElementById('orderStatusFilter') ? document.getElementById('orderStatusFilter').value : 'ALL';
    const q = document.getElementById('orderSearchInput') ? document.getElementById('orderSearchInput').value.trim() : '';
    let url = `/api/admin/orders?status=${encodeURIComponent(filter)}`;
    if (q) url += `&q=${encodeURIComponent(q)}`;

    const res = await fetch(url, {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const orders = await res.json();
    STATE.adminOrdersCache = orders;

    const tbody = document.getElementById('supplementOrdersTableBody');
    if (!tbody) return;

    if (orders.length === 0) {
      tbody.innerHTML = `<tr><td colspan="9" class="p-8 text-center text-slate-500">No supplement orders found.</td></tr>`;
      return;
    }

    tbody.innerHTML = orders.map(o => {
      const member = o.member;
      const user = o.user;
      const name = member ? member.fullName : (user ? user.fullName : 'Guest');
      const code = member ? member.memberCode : `USR-${o.user.id}`;
      const phone = member ? member.phoneNumber : (user ? user.username : '-');
      const photo = member && member.photoUrl ? member.photoUrl : 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100';

      const supp = o.supplement;
      const suppName = supp ? supp.name : 'Supplement';
      const suppImg = supp && supp.imageUrl ? supp.imageUrl : 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=100';

      const orderDate = new Date(o.orderDate).toLocaleDateString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
      });

      // Status pill
      let statusBadge = '';
      if (o.orderStatus === 'COLLECTED') {
        statusBadge = '<span class="px-2 py-1 rounded bg-gym-emerald/20 text-gym-emerald border border-gym-emerald/40 font-bold text-[10px]">✓ COLLECTED</span>';
      } else if (o.orderStatus === 'READY_FOR_COLLECTION') {
        statusBadge = '<span class="px-2 py-1 rounded bg-purple-950/80 text-purple-300 border border-purple-800/80 font-bold text-[10px]">READY FOR PICKUP</span>';
      } else if (o.orderStatus === 'PAID') {
        statusBadge = '<span class="px-2 py-1 rounded bg-blue-950/80 text-blue-300 border border-blue-800/80 font-bold text-[10px]">PAID</span>';
      } else if (o.orderStatus === 'PAYMENT_PENDING') {
        statusBadge = '<span class="px-2 py-1 rounded bg-amber-950/80 text-amber-300 border border-amber-800/80 font-bold text-[10px]">PAYMENT PENDING</span>';
      } else {
        statusBadge = `<span class="px-2 py-1 rounded bg-slate-800 text-slate-400 font-bold text-[10px]">${o.orderStatus}</span>`;
      }

      // Payment pill
      const isPaid = o.paymentStatus === 'PAID';
      const payBadge = isPaid
        ? '<span class="px-2 py-0.5 rounded bg-gym-emerald/20 text-gym-emerald text-[10px] font-bold">PAID</span>'
        : '<span class="px-2 py-0.5 rounded bg-red-950/80 text-red-300 text-[10px] font-bold">PENDING</span>';

      // Actions
      let actionHtml = '';
      if (o.orderStatus === 'COLLECTED') {
        actionHtml = `<span class="text-xs text-slate-500 font-medium">Completed</span>`;
      } else {
        actionHtml = `
          <div class="flex items-center justify-center gap-1.5">
            ${!isPaid ? `
              <button onclick="verifyOrderPayment(${o.id})" class="px-2.5 py-1.5 bg-blue-600 hover:bg-blue-500 text-white rounded-lg text-[10px] font-bold uppercase transition" title="Verify Payment">
                Verify Pay
              </button>
            ` : ''}
            <button onclick="openCollectionModal(${o.id})" class="px-3 py-1.5 bg-gym-orange hover:bg-gym-orangeHover text-white rounded-lg text-[10px] font-bold uppercase shadow-md shadow-gym-orangeGlow transition flex items-center gap-1">
              <span>📸 Collect Product</span>
            </button>
          </div>
        `;
      }

      return `
        <tr class="hover:bg-slate-900/50 transition">
          <td class="p-4 font-mono font-bold text-gym-orange">
            ${o.orderNumber}
            ${o.paymentReceiptNumber ? `<span class="block text-[10px] text-slate-400 font-mono font-normal">Pay: ${o.paymentReceiptNumber}</span>` : ''}
          </td>
          <td class="p-4">
            <div class="flex items-center gap-2.5">
              <img src="${photo}" class="w-8 h-8 rounded-lg object-cover border border-gym-border">
              <div>
                <p class="font-bold text-white text-xs">${name}</p>
                <p class="text-[10px] text-slate-400 font-mono">${code} • ${phone}</p>
              </div>
            </div>
          </td>
          <td class="p-4">
            <div class="flex items-center gap-2">
              <img src="${suppImg}" class="w-8 h-8 rounded-lg object-cover border border-gym-border">
              <span class="font-medium text-white max-w-[180px] truncate block">${suppName}</span>
            </div>
          </td>
          <td class="p-4 font-mono font-bold text-white text-center">${o.quantity}</td>
          <td class="p-4">
            <span class="font-display font-bold text-white text-sm">₹${Math.round(o.totalAmount).toLocaleString('en-IN')}</span>
          </td>
          <td class="p-4 text-slate-400 text-[11px] whitespace-nowrap">${orderDate}</td>
          <td class="p-4">
            ${payBadge}
            ${o.upiTransactionRef ? `<span class="block text-[10px] text-slate-400 font-mono mt-1">UTR: ${o.upiTransactionRef}</span>` : ''}
          </td>
          <td class="p-4">${statusBadge}</td>
          <td class="p-4 text-center">${actionHtml}</td>
        </tr>
      `;
    }).join('');
  } catch (e) {
    console.error('Error loading supplement orders', e);
  }
}

async function verifyOrderPayment(orderId) {
  try {
    const res = await fetch(`/api/admin/orders/${orderId}/verify-payment`, {
      method: 'PATCH',
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) {
      showToast('Failed to verify payment', 'error');
      return;
    }
    showToast('Payment confirmed! Product ready for collection.', 'success');
    loadSupplementOrders();
    if (typeof loadPayments === 'function') {
      loadPayments();
    }
    if (typeof loadDailyCollection === 'function') {
      loadDailyCollection();
    }
  } catch (e) {
    showToast('Network error verifying payment', 'error');
  }
}

// --- 3. FRONT DESK COLLECTION & DEVICE CAMERA ENGINE ---
let collectionCameraStream = null;
let capturedCollectionPhotoBase64 = null;
let activeCollectionOrder = null;

function openCollectionModal(orderId) {
  if (!STATE.adminOrdersCache) return;
  const o = STATE.adminOrdersCache.find(item => item.id == orderId);
  if (!o) return;

  activeCollectionOrder = o;
  capturedCollectionPhotoBase64 = null;

  document.getElementById('colOrderId').value = o.id;
  const member = o.member;
  const user = o.user;
  document.getElementById('colMemberName').textContent = member ? member.fullName : (user ? user.fullName : 'Guest');
  document.getElementById('colMemberCode').textContent = member ? member.memberCode : `USR-${o.user.id}`;
  document.getElementById('colMemberPhone').textContent = member ? member.phoneNumber : (user ? user.username : '-');
  document.getElementById('colMemberPhoto').src = member && member.photoUrl ? member.photoUrl : 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100';

  document.getElementById('colOrderNumber').textContent = o.orderNumber;
  document.getElementById('colProductName').textContent = o.supplement ? o.supplement.name : 'Supplement';
  document.getElementById('colQuantity').textContent = `${o.quantity} unit(s)`;
  document.getElementById('colTotalAmount').textContent = `₹${Math.round(o.totalAmount).toLocaleString('en-IN')}`;
  document.getElementById('colNotes').value = '';

  // Reset Camera View UI
  document.getElementById('colCameraVideo').classList.add('hidden');
  document.getElementById('colCapturedPreview').classList.add('hidden');
  document.getElementById('colCameraPlaceholder').classList.remove('hidden');
  document.getElementById('colLiveBadge').classList.add('hidden');

  document.getElementById('colStartCamBtn').classList.remove('hidden');
  document.getElementById('colCaptureBtn').classList.add('hidden');
  document.getElementById('colRetakeBtn').classList.add('hidden');

  document.getElementById('collectionModal').classList.remove('hidden');
}

function closeCollectionModal() {
  stopCollectionCamera();
  document.getElementById('collectionModal').classList.add('hidden');
  activeCollectionOrder = null;
  capturedCollectionPhotoBase64 = null;
}

async function startCollectionCamera() {
  try {
    const video = document.getElementById('colCameraVideo');
    const placeholder = document.getElementById('colCameraPlaceholder');
    const liveBadge = document.getElementById('colLiveBadge');
    const preview = document.getElementById('colCapturedPreview');

    preview.classList.add('hidden');
    capturedCollectionPhotoBase64 = null;

    collectionCameraStream = await navigator.mediaDevices.getUserMedia({
      video: {
        facingMode: 'environment',
        width: { ideal: 1280 },
        height: { ideal: 720 }
      },
      audio: false
    });

    video.srcObject = collectionCameraStream;
    video.classList.remove('hidden');
    placeholder.classList.add('hidden');
    liveBadge.classList.remove('hidden');

    document.getElementById('colStartCamBtn').classList.add('hidden');
    document.getElementById('colCaptureBtn').classList.remove('hidden');
    document.getElementById('colRetakeBtn').classList.add('hidden');
  } catch (err) {
    console.error('Camera access error:', err);
    showToast('Camera not available. Please use the "Upload File" option.', 'info');
  }
}

function captureCollectionPhoto() {
  const video = document.getElementById('colCameraVideo');
  const canvas = document.getElementById('colCameraCanvas');
  const preview = document.getElementById('colCapturedPreview');
  const liveBadge = document.getElementById('colLiveBadge');

  canvas.width = video.videoWidth || 640;
  canvas.height = video.videoHeight || 480;
  const ctx = canvas.getContext('2d');
  ctx.drawImage(video, 0, 0, canvas.width, canvas.height);

  capturedCollectionPhotoBase64 = canvas.toDataURL('image/jpeg', 0.85);

  // Stop camera stream
  stopCollectionCamera();

  // Show captured photo preview
  video.classList.add('hidden');
  liveBadge.classList.add('hidden');
  preview.src = capturedCollectionPhotoBase64;
  preview.classList.remove('hidden');

  document.getElementById('colCaptureBtn').classList.add('hidden');
  document.getElementById('colRetakeBtn').classList.remove('hidden');
  showToast('Member photo captured! Ready to confirm collection.', 'success');
}

function retakeCollectionPhoto() {
  startCollectionCamera();
}

function stopCollectionCamera() {
  if (collectionCameraStream) {
    collectionCameraStream.getTracks().forEach(track => track.stop());
    collectionCameraStream = null;
  }
}

function handleCollectionFileSelect(e) {
  const file = e.target.files[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = function(evt) {
    capturedCollectionPhotoBase64 = evt.target.result;

    const video = document.getElementById('colCameraVideo');
    const placeholder = document.getElementById('colCameraPlaceholder');
    const liveBadge = document.getElementById('colLiveBadge');
    const preview = document.getElementById('colCapturedPreview');

    stopCollectionCamera();
    video.classList.add('hidden');
    placeholder.classList.add('hidden');
    liveBadge.classList.add('hidden');

    preview.src = capturedCollectionPhotoBase64;
    preview.classList.remove('hidden');

    document.getElementById('colStartCamBtn').classList.add('hidden');
    document.getElementById('colCaptureBtn').classList.add('hidden');
    document.getElementById('colRetakeBtn').classList.remove('hidden');

    showToast('Hand-over photo loaded from file.', 'success');
  };
  reader.readAsDataURL(file);
}

async function confirmCollectionSubmit() {
  if (!activeCollectionOrder) return;
  if (!capturedCollectionPhotoBase64) {
    showToast('Please take member photo with camera first!', 'error');
    return;
  }

  const orderId = activeCollectionOrder.id;
  const notes = document.getElementById('colNotes').value.trim();
  const btn = document.getElementById('confirmCollectionSubmitBtn');

  btn.disabled = true;
  btn.innerHTML = '<span>Logging Hand-over...</span>';

  try {
    const res = await fetch(`/api/admin/orders/${orderId}/collect`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${STATE.token}`
      },
      body: JSON.stringify({
        collectionPhotoBase64: capturedCollectionPhotoBase64,
        notes: notes
      })
    });

    const data = await res.json();
    if (!res.ok) {
      showToast(data.error || 'Failed to record collection', 'error');
      btn.disabled = false;
      btn.innerHTML = '<span>✓ Confirm Collection</span>';
      return;
    }

    closeCollectionModal();
    showToast('Product hand-over completed! Stock updated.', 'success');
    loadSupplementOrders();
    loadSupplements();
    loadAdminCollectionHistory();
  } catch (e) {
    showToast('Network error during collection submission', 'error');
  } finally {
    if (btn) {
      btn.disabled = false;
      btn.innerHTML = '<span>✓ Confirm Collection</span>';
    }
  }
}

// --- 4. ADMIN: SUPPLEMENT COLLECTION HISTORY ---
async function loadAdminCollectionHistory() {
  try {
    const res = await fetch('/api/admin/collections', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const collections = await res.json();

    const grid = document.getElementById('adminCollectionGrid');
    if (!grid) return;

    if (collections.length === 0) {
      grid.innerHTML = '<p class="col-span-3 text-center py-12 text-slate-500">No collection records found yet.</p>';
      return;
    }

    grid.innerHTML = collections.map(c => `
      <div class="bg-gym-card border border-gym-border rounded-2xl p-5 shadow-xl space-y-4 relative overflow-hidden">
        <!-- Header -->
        <div class="flex items-center justify-between pb-3 border-b border-gym-border">
          <div>
            <span class="text-[10px] font-mono font-bold text-gym-orange block">${c.collectionNumber}</span>
            <span class="text-xs font-bold text-white">${c.collectionDateFormatted} • ${c.collectionTimeFormatted}</span>
          </div>
          <span class="px-2 py-0.5 rounded bg-gym-emerald/20 text-gym-emerald border border-gym-emerald/40 text-[10px] font-bold">
            ✓ COLLECTED
          </span>
        </div>

        <!-- Member and Order Details -->
        <div class="flex items-center gap-3">
          <img src="${c.memberPhotoUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100'}" class="w-12 h-12 rounded-xl object-cover border border-gym-border">
          <div class="flex-1 min-w-0">
            <h5 class="font-bold text-white text-sm truncate">${c.memberName}</h5>
            <p class="text-[11px] text-slate-400 font-mono">${c.memberCode || ''} • ${c.phoneNumber || ''}</p>
            <p class="text-[10px] text-gym-orange font-bold uppercase mt-0.5">Order: ${c.order ? c.order.orderNumber : ''}</p>
          </div>
        </div>

        <!-- Supplement Info -->
        <div class="p-3 rounded-xl bg-slate-900 border border-gym-border flex items-center justify-between text-xs">
          <div class="flex items-center gap-2">
            <img src="${c.supplementImageUrl || 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=100'}" class="w-8 h-8 rounded-lg object-cover">
            <div>
              <p class="font-bold text-white truncate max-w-[140px]">${c.supplementName}</p>
              <span class="text-[10px] text-slate-400">${c.quantity}x unit(s)</span>
            </div>
          </div>
          <span class="font-display font-bold text-base text-gym-orange">₹${Math.round(c.totalAmount).toLocaleString('en-IN')}</span>
        </div>

        <!-- Captured Hand-over Photo -->
        <div class="space-y-1.5">
          <span class="text-[10px] font-bold uppercase text-slate-400 tracking-wider flex items-center gap-1">
            <span>📸</span> Front Desk Hand-Over Photo
          </span>
          <div class="h-36 rounded-xl overflow-hidden border border-gym-border bg-slate-950">
            <img src="${c.collectionPhotoUrl}" alt="Hand-over Proof" class="w-full h-full object-cover">
          </div>
        </div>

        <div class="pt-2 border-t border-gym-border/60 flex items-center justify-between text-[10px] text-slate-500">
          <span>Staff: ${c.collectedByStaff || 'Front Desk'}</span>
          <span class="text-gym-emerald font-bold">Physical Verification Complete</span>
        </div>
      </div>
    `).join('');
  } catch (e) {
    console.error('Error loading collection history', e);
  }
}

// --- 5. USER: MY SUPPLEMENT ORDERS ---
async function loadUserOrders() {
  try {
    const res = await fetch('/api/orders/my-orders', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const orders = await res.json();

    const container = document.getElementById('userOrdersList');
    if (!container) return;

    if (orders.length === 0) {
      container.innerHTML = `
        <div class="text-center py-16 bg-gym-card border border-gym-border rounded-2xl p-6">
          <p class="text-slate-400 text-sm">You haven't ordered any gym supplements yet.</p>
          <button onclick="navigateTo('supplements')" class="mt-4 bg-gym-orange hover:bg-gym-orangeHover text-white px-4 py-2 rounded-xl text-xs font-bold uppercase shadow-md shadow-gym-orangeGlow transition">
            Explore Supplement Store
          </button>
        </div>
      `;
      return;
    }

    container.innerHTML = orders.map(o => {
      const supp = o.supplement;
      const date = new Date(o.orderDate).toLocaleDateString('en-IN', {
        day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit'
      });

      let statusNotice = '';
      if (o.orderStatus === 'COLLECTED') {
        statusNotice = `<span class="px-2.5 py-1 rounded-md bg-gym-emerald/20 text-gym-emerald border border-gym-emerald/40 text-xs font-bold">✓ COLLECTED</span>`;
      } else if (o.orderStatus === 'READY_FOR_COLLECTION') {
        statusNotice = `<span class="px-2.5 py-1 rounded-md bg-purple-950 text-purple-300 border border-purple-700 text-xs font-bold animate-pulse">🎉 Ready at Front Desk</span>`;
      } else if (o.orderStatus === 'PAID') {
        statusNotice = `<span class="px-2.5 py-1 rounded-md bg-blue-950 text-blue-300 border border-blue-700 text-xs font-bold">Payment Verified</span>`;
      } else {
        statusNotice = `<span class="px-2.5 py-1 rounded-md bg-amber-950 text-amber-300 border border-amber-700 text-xs font-bold">Payment Pending</span>`;
      }

      return `
        <div class="bg-gym-card border border-gym-border rounded-2xl p-5 shadow-xl flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
          <div class="flex items-center gap-4">
            <img src="${supp && supp.imageUrl ? supp.imageUrl : 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=100'}" class="w-16 h-16 rounded-xl object-cover border border-gym-border">
            <div>
              <div class="flex items-center gap-2">
                <span class="font-mono font-bold text-gym-orange text-xs">${o.orderNumber}</span>
                <span class="text-[11px] text-slate-500">• ${date}</span>
              </div>
              <h4 class="font-display font-bold text-base text-white mt-0.5">${supp ? supp.name : 'Supplement'}</h4>
              <p class="text-xs text-slate-400 mt-0.5">Quantity: <span class="font-bold text-white">${o.quantity}</span> • Method: <span class="font-bold text-white">${o.paymentMethod}</span></p>
            </div>
          </div>

          <div class="flex flex-row sm:flex-col items-center sm:items-end justify-between w-full sm:w-auto gap-2 border-t sm:border-t-0 border-gym-border pt-3 sm:pt-0">
            <span class="font-display font-bold text-xl text-gym-orange">₹${Math.round(o.totalAmount).toLocaleString('en-IN')}</span>
            ${statusNotice}
          </div>
        </div>
      `;
    }).join('');
  } catch (e) {
    console.error('Error loading user orders', e);
  }
}

// --- 6. USER: MY COLLECTION HISTORY ---
async function loadUserCollectionHistory() {
  try {
    const res = await fetch('/api/orders/my-collections', {
      headers: { 'Authorization': `Bearer ${STATE.token}` }
    });
    if (!res.ok) return;
    const collections = await res.json();

    const grid = document.getElementById('userCollectionGrid');
    if (!grid) return;

    if (collections.length === 0) {
      grid.innerHTML = '<p class="col-span-3 text-center py-12 text-slate-500">You do not have any completed collections yet.</p>';
      return;
    }

    grid.innerHTML = collections.map(c => `
      <div class="bg-gym-card border border-gym-border rounded-2xl p-5 shadow-xl space-y-4">
        <div class="flex items-center justify-between pb-3 border-b border-gym-border">
          <div>
            <span class="text-[10px] font-mono font-bold text-gym-orange block">${c.collectionNumber}</span>
            <span class="text-xs font-bold text-white">${c.collectionDateFormatted} • ${c.collectionTimeFormatted}</span>
          </div>
          <span class="px-2 py-0.5 rounded bg-gym-emerald/20 text-gym-emerald border border-gym-emerald/40 text-[10px] font-bold">
            ✓ COLLECTED
          </span>
        </div>

        <div class="flex items-center gap-3">
          <img src="${c.supplementImageUrl || 'https://images.unsplash.com/photo-1579722821273-0f6c7d44362f?w=100'}" class="w-12 h-12 rounded-xl object-cover border border-gym-border">
          <div>
            <h5 class="font-bold text-white text-sm">${c.supplementName}</h5>
            <p class="text-xs text-slate-400">${c.quantity}x unit(s) • Total: <span class="font-bold text-gym-orange">₹${Math.round(c.totalAmount).toLocaleString('en-IN')}</span></p>
          </div>
        </div>

        <div class="space-y-1.5">
          <span class="text-[10px] font-bold uppercase text-slate-400 tracking-wider">Verified Hand-Over Photo</span>
          <div class="h-44 rounded-xl overflow-hidden border border-gym-border bg-slate-950">
            <img src="${c.collectionPhotoUrl}" class="w-full h-full object-cover">
          </div>
        </div>
      </div>
    `).join('');
  } catch (e) {
    console.error('Error loading user collection history', e);
  }
}
