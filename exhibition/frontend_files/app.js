const API_URL = '/api';
let currentAuthMode = 'login';
let selectedPaintingId = null;
let selectedPaintingTitle = '';
let selectedPaintingAuthor = '';
let userFavoritesIds = new Set();
let paintingsCache = [];
let pendingVerificationEmail = '';
let chatSending = false;
let activeProfileTab = 'favorites';
let discussionReturnToProfile = false;

document.addEventListener('DOMContentLoaded', () => {
    checkAuthStatus();
    loadPaintingStylesForFilters();
    applyGallerySearch();
    const si = document.getElementById('searchInput');
    si.addEventListener('keydown', (e) => {
        if (e.key === 'Enter') applyGallerySearch();
    });
    ['filterStyle', 'filterSort'].forEach((id) => {
        document.getElementById(id).addEventListener('change', () => applyGallerySearch());
    });
    document.getElementById('filterAuthor').addEventListener(
        'keydown',
        (e) => {
            if (e.key === 'Enter') applyGallerySearch();
        }
    );
});

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    toast.setAttribute('role', 'status');
    toast.innerHTML = `<span class="toast-msg">${escapeHtml(message)}</span>`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.classList.add('toast-out');
        toast.addEventListener('animationend', () => toast.remove(), { once: true });
    }, 3800);
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function setButtonLoading(btn, loading, labelWhenDone) {
    if (!btn) return;
    if (loading) {
        btn.dataset.originalText = btn.innerHTML;
        btn.disabled = true;
        btn.classList.add('btn-loading');
        btn.innerHTML = '<span class="spinner-inline"></span> Подождите…';
    } else {
        btn.disabled = false;
        btn.classList.remove('btn-loading');
        btn.innerHTML = labelWhenDone ?? btn.dataset.originalText ?? btn.innerHTML;
        delete btn.dataset.originalText;
    }
}

async function loadPaintingStylesForFilters() {
    try {
        const res = await fetch(`${API_URL}/paintings/meta/styles`);
        if (!res.ok) return;
        const styles = await res.json();
        const sel = document.getElementById('filterStyle');
        const keep = sel.querySelector('option[value=""]');
        sel.innerHTML = '';
        sel.appendChild(keep);
        styles.forEach((s) => {
            const opt = document.createElement('option');
            opt.value = s;
            opt.textContent = s;
            sel.appendChild(opt);
        });
    } catch (e) {
        /* ignore */
    }
}

function buildSearchQueryParams() {
    const params = new URLSearchParams();
    const q = document.getElementById('searchInput').value.trim();
    const style = document.getElementById('filterStyle').value;
    const author = document.getElementById('filterAuthor').value.trim();
    const sort = document.getElementById('filterSort').value;
    if (q) params.set('q', q);
    if (style) params.set('style', style);
    if (author) params.set('author', author);
    if (sort) params.set('sort', sort);
    return params;
}

function updateFiltersBadge() {
    const q = document.getElementById('searchInput').value.trim();
    const style = document.getElementById('filterStyle').value;
    const author = document.getElementById('filterAuthor').value.trim();
    const sort = document.getElementById('filterSort').value;
    let n = 0;
    if (q) n++;
    if (style) n++;
    if (author) n++;
    if (sort && sort !== 'TITLE_ASC') n++;
    const badge = document.getElementById('filtersBadge');
    if (n > 0) {
        badge.textContent = String(n);
        badge.classList.remove('hidden');
    } else {
        badge.textContent = '';
        badge.classList.add('hidden');
    }
}

async function applyGallerySearch() {
    const loader = document.getElementById('galleryLoader');
    const hint = document.getElementById('searchResultsHint');
    const params = buildSearchQueryParams();
    updateFiltersBadge();

    try {
        if (loader) {
            loader.style.display = 'block';
            loader.innerHTML = '<span class="spinner-inline"></span> Загрузка…';
        }
        const url = `${API_URL}/paintings/search?${params.toString()}`;
        const response = await fetch(url);
        if (!response.ok) {
            if (loader) loader.innerText = 'Не удалось выполнить поиск';
            hint.innerText = '';
            return;
        }
        const paintings = await response.json();
        paintingsCache = paintings;
        renderGallery(paintings, 'homeView');
        if (loader) loader.style.display = 'none';
        const total = paintings.length;
        hint.innerText =
            total === 0
                ? 'Ничего не найдено — измените запрос или сбросьте фильтры.'
                : `Найдено работ: ${total}`;
    } catch (e) {
        if (loader) loader.innerText = 'Ошибка соединения с сервером';
        hint.innerText = '';
    }
}

function resetGalleryFilters() {
    document.getElementById('searchInput').value = '';
    document.getElementById('filterStyle').value = '';
    document.getElementById('filterAuthor').value = '';
    document.getElementById('filterSort').value = 'TITLE_ASC';
    applyGallerySearch();
}

function toggleFiltersPanel() {
    const panel = document.getElementById('filtersPanel');
    const btn = document.getElementById('filtersToggle');
    const nowHidden = panel.classList.toggle('hidden');
    btn.setAttribute('aria-expanded', String(!nowHidden));
}

async function fetchFavorites() {
    const token = localStorage.getItem('jwt_token');
    if (!token) return;

    try {
        const response = await fetch(`${API_URL}/users/favorites`, {
            headers: { Authorization: `Bearer ${token.replace(/['"]+/g, '')}` },
        });
        if (response.ok) {
            const favorites = await response.json();
            userFavoritesIds = new Set(favorites.map((p) => p.id));
            renderGallery(favorites, 'favoritesGallery');
        }
    } catch (e) {
        showToast('Не удалось загрузить избранное', 'error');
    }
}

async function toggleFavorite() {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        showToast('Для добавления в избранное нужно войти в систему', 'error');
        openAuthModal('login');
        return;
    }

    try {
        const response = await fetch(`${API_URL}/users/favorites/${selectedPaintingId}`, {
            method: 'POST',
            headers: { Authorization: `Bearer ${token.replace(/['"]+/g, '')}` },
        });

        if (response.ok) {
            const msg = await response.text();
            showToast(msg, 'success');

            const btn = document.getElementById('favoriteBtn');
            if (userFavoritesIds.has(selectedPaintingId)) {
                userFavoritesIds.delete(selectedPaintingId);
                btn.classList.remove('liked');
                btn.innerText = '🤍 В избранное';
            } else {
                userFavoritesIds.add(selectedPaintingId);
                btn.classList.add('liked');
                btn.innerText = '❤️ В избранном';
            }

            fetchFavorites();
        }
    } catch (e) {
        showToast('Ошибка при обновлении избранного', 'error');
    }
}

function renderGallery(paintings, targetElementId) {
    const gridHome = document.getElementById('galleryGrid');
    const loaderHome = document.getElementById('galleryLoader');

    if (targetElementId === 'homeView') {
        if (gridHome) gridHome.innerHTML = '';
        if (loaderHome) loaderHome.style.display = 'none';
    } else {
        document.getElementById('favoritesGallery').innerHTML = '';
    }

    if (paintings.length === 0) {
        const emptyEl =
            targetElementId === 'homeView'
                ? gridHome
                : document.getElementById('favoritesGallery');
        if (emptyEl) {
            emptyEl.innerHTML = '<p class="gallery-empty">Тут пока пусто 🎨</p>';
        }
        return;
    }

    const targetGallery =
        targetElementId === 'homeView' ? gridHome : document.getElementById('favoritesGallery');

    paintings.forEach((p) => {
        const card = document.createElement('div');
        card.className = 'card';
        const imgUrl = p.imageUrl || 'https://via.placeholder.com/400x300?text=Нет+Изображения';
        card.innerHTML = `
            <div class="card-image-wrap">
                <img src="${imgUrl}" alt="${escapeHtml(p.title || '')}" loading="lazy">
            </div>
            <div class="card-info"><h3>${escapeHtml(p.title || '')}</h3><p>${escapeHtml(p.author || '')}</p></div>`;
        card.onclick = () => openPaintingModal(p);
        targetGallery.appendChild(card);
    });
}

function switchView(view) {
    document.getElementById('homeView').style.display = view === 'home' ? 'block' : 'none';
    document.getElementById('profileView').style.display = view === 'profile' ? 'block' : 'none';
    closePaintingModal();
    if (view === 'profile') {
        switchProfileTab(activeProfileTab);
    }
}

function switchProfileTab(tab) {
    activeProfileTab = tab;
    const favBtn = document.getElementById('profileTabFavoritesBtn');
    const comBtn = document.getElementById('profileTabCommentsBtn');
    const favPanel = document.getElementById('profilePanelFavorites');
    const comPanel = document.getElementById('profilePanelComments');
    const isFavorites = tab === 'favorites';

    favBtn.classList.toggle('profile-tab-active', isFavorites);
    comBtn.classList.toggle('profile-tab-active', !isFavorites);
    favBtn.setAttribute('aria-selected', isFavorites ? 'true' : 'false');
    comBtn.setAttribute('aria-selected', !isFavorites ? 'true' : 'false');
    favPanel.style.display = isFavorites ? 'block' : 'none';
    comPanel.style.display = isFavorites ? 'none' : 'block';

    if (isFavorites) {
        fetchFavorites();
    } else {
        fetchMyComments();
    }
}

function checkAuthStatus() {
    const token = localStorage.getItem('jwt_token');
    if (token) {
        document.getElementById('guestMenu').style.display = 'none';
        document.getElementById('userMenu').style.display = 'flex';
        try {
            const payload = JSON.parse(atob(token.split('.')[1]));
            const displayName = payload.username || payload.name || payload.sub;
            document.getElementById('userNameDisplay').innerText = `Привет, ${displayName}!`;
            fetchFavorites();
        } catch (e) {
            logout();
        }
    } else {
        document.getElementById('guestMenu').style.display = 'block';
        document.getElementById('userMenu').style.display = 'none';
        userFavoritesIds.clear();
    }
}

async function submitAuth() {
    const email = document.getElementById('authEmail').value.trim();
    const password = document.getElementById('authPassword').value;
    const username = document.getElementById('authUsername').value.trim();
    const btn = document.getElementById('authSubmitBtn');
    const hint = document.getElementById('authHint');

    const endpoint = currentAuthMode === 'login' ? '/auth/login' : '/auth/register';
    const payload =
        currentAuthMode === 'login' ? { email, password } : { email, username, password };

    hint.innerText = '';

    try {
        setButtonLoading(btn, true);
        const response = await fetch(`${API_URL}${endpoint}`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload),
        });

        const bodyText = await response.text();

        if (!response.ok) {
            showToast(bodyText || 'Ошибка', 'error');
            setButtonLoading(btn, false, currentAuthMode === 'login' ? 'Войти' : 'Зарегистрироваться');
            return;
        }

        if (currentAuthMode === 'login') {
            const data = JSON.parse(bodyText);
            let rawToken = data.token || data.toker;
            if (!rawToken) throw new Error('Сервер не вернул токен');
            const cleanToken = rawToken.replace(/['"]+/g, '');
            localStorage.setItem('jwt_token', cleanToken);
            closeAuthModal();
            showToast('Успешный вход!', 'success');
            checkAuthStatus();
            setButtonLoading(btn, false, 'Войти');
        } else {
            pendingVerificationEmail = email;
            closeAuthModal();
            const regToastType = response.status === 202 ? 'info' : 'success';
            showToast(bodyText || 'Письмо с кодом отправлено', regToastType);
            openVerifyModal();
            setButtonLoading(btn, false, 'Зарегистрироваться');
        }
    } catch (e) {
        showToast('Сетевая ошибка', 'error');
        setButtonLoading(
            btn,
            false,
            currentAuthMode === 'login' ? 'Войти' : 'Зарегистрироваться'
        );
    }
}

function openVerifyModal() {
    document.getElementById('verifyModal').style.display = 'flex';
    document.getElementById('verifyEmailDisplay').innerText = pendingVerificationEmail;
    document.getElementById('verifyOtpInput').value = '';
    document.getElementById('verifyHint').innerText = '';
}

function closeVerifyModal() {
    document.getElementById('verifyModal').style.display = 'none';
}

async function submitVerification() {
    const otpCode = document.getElementById('verifyOtpInput').value.trim();
    const btn = document.getElementById('verifySubmitBtn');
    const hint = document.getElementById('verifyHint');

    if (!/^\d{6}$/.test(otpCode)) {
        hint.innerText = 'Введите ровно 6 цифр';
        return;
    }

    hint.innerText = '';
    try {
        setButtonLoading(btn, true);
        const response = await fetch(`${API_URL}/auth/verify`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ email: pendingVerificationEmail, otpCode }),
        });
        const text = await response.text();
        if (!response.ok) {
            showToast(text || 'Ошибка подтверждения', 'error');
            setButtonLoading(btn, false, 'Подтвердить');
            return;
        }
        showToast(text || 'Email подтверждён', 'success');
        closeVerifyModal();
        openAuthModal('login');
        document.getElementById('authEmail').value = pendingVerificationEmail;
        document.getElementById('authHint').innerText = 'Теперь войдите с вашим паролем.';
        setButtonLoading(btn, false, 'Подтвердить');
    } catch (e) {
        showToast('Сетевая ошибка', 'error');
        setButtonLoading(btn, false, 'Подтвердить');
    }
}

function logout() {
    localStorage.removeItem('jwt_token');
    checkAuthStatus();
    switchView('home');
    showToast('Вы вышли из системы', 'info');
}

function openAuthModal(mode) {
    currentAuthMode = mode;
    document.getElementById('authModal').style.display = 'flex';
    document.getElementById('authTitle').innerText = mode === 'login' ? 'Вход в систему' : 'Регистрация';
    document.getElementById('authSubmitBtn').innerText = mode === 'login' ? 'Войти' : 'Зарегистрироваться';
    document.getElementById('authUsername').style.display = mode === 'login' ? 'none' : 'block';
    document.getElementById('authHint').innerText = '';
}

function closeAuthModal() {
    document.getElementById('authModal').style.display = 'none';
}

function openPaintingModal(p) {
    selectedPaintingId = p.id;
    selectedPaintingTitle = p.title || '';
    selectedPaintingAuthor = p.author || '';

    document.getElementById('paintingModal').style.display = 'flex';
    document.getElementById('modalImage').src = p.imageUrl || '';
    document.getElementById('modalTitle').innerText = p.title || '';
    document.getElementById('modalAuthor').innerText = p.author || '';
    document.getElementById('modalStyle').innerText = p.style || '';
    document.getElementById('modalDesc').innerText = p.description || '';

    document.getElementById('chatBox').innerHTML =
        '<div class="message ai-message">Привет! Я готов рассказать об этой картине.</div>';

    const btn = document.getElementById('favoriteBtn');
    if (userFavoritesIds.has(p.id)) {
        btn.classList.add('liked');
        btn.innerText = '❤️ В избранном';
    } else {
        btn.classList.remove('liked');
        btn.innerText = '🤍 В избранное';
    }
}

function closePaintingModal() {
    document.getElementById('discussionModal').style.display = 'none';
    document.getElementById('paintingModal').style.display = 'none';
}

function openDiscussionModal() {
    if (!selectedPaintingId) return;
    document.getElementById('discussionTitle').innerText = `Обсуждение: ${selectedPaintingTitle}`;
    document.getElementById('discussionSubtitle').innerText = selectedPaintingAuthor
        ? `Автор: ${selectedPaintingAuthor}`
        : '';
    document.getElementById('commentInput').value = '';
    document.getElementById('commentHint').innerText = '';
    document.getElementById('paintingModal').style.display = 'none';
    document.getElementById('discussionModal').style.display = 'flex';
    loadComments();
}

function closeDiscussionModal() {
    const discussion = document.getElementById('discussionModal');
    if (discussion) discussion.style.display = 'none';
    if (discussionReturnToProfile) {
        discussionReturnToProfile = false;
        document.getElementById('paintingModal').style.display = 'none';
        switchView('profile');
        if (activeProfileTab === 'comments') {
            fetchMyComments();
        }
    } else {
        document.getElementById('paintingModal').style.display = 'flex';
    }
}

async function loadComments() {
    const list = document.getElementById('commentsList');
    list.innerHTML = '<p class="comments-loading">Загрузка комментариев…</p>';
    try {
        const response = await fetch(`${API_URL}/paintings/${selectedPaintingId}/comments`);
        if (!response.ok) {
            list.innerHTML = '<p class="comments-empty">Не удалось загрузить комментарии</p>';
            return;
        }
        const comments = await response.json();
        renderComments(comments);
    } catch (e) {
        list.innerHTML = '<p class="comments-empty">Ошибка соединения</p>';
    }
}

function renderComments(comments) {
    const list = document.getElementById('commentsList');
    if (!comments || comments.length === 0) {
        list.innerHTML = '<p class="comments-empty">Пока нет комментариев. Будьте первым!</p>';
        return;
    }
    list.innerHTML = comments
        .map(
            (c) => `
        <article class="comment-item">
            <div class="comment-author">${escapeHtml(c.username)}</div>
            <p class="comment-text">${escapeHtml(c.content)}</p>
        </article>`
        )
        .join('');
    list.scrollTop = list.scrollHeight;
}

async function fetchMyComments() {
    const list = document.getElementById('myCommentsList');
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        list.innerHTML = '<p class="comments-empty">Войдите, чтобы видеть свои комментарии</p>';
        return;
    }
    list.innerHTML = '<p class="comments-loading">Загрузка комментариев…</p>';
    try {
        const response = await fetch(`${API_URL}/users/comments`, {
            headers: { Authorization: `Bearer ${token.replace(/['"]+/g, '')}` },
        });
        if (!response.ok) {
            const text = await response.text();
            list.innerHTML = `<p class="comments-empty">${escapeHtml(text || 'Не удалось загрузить')}</p>`;
            return;
        }
        const comments = await response.json();
        renderMyComments(comments);
    } catch (e) {
        list.innerHTML = '<p class="comments-empty">Ошибка соединения</p>';
    }
}

function renderMyComments(comments) {
    const list = document.getElementById('myCommentsList');
    if (!comments || comments.length === 0) {
        list.innerHTML = '<p class="comments-empty">Вы ещё не оставляли комментариев</p>';
        return;
    }
    list.innerHTML = comments
        .map((c) => {
            const commentId = String(c.id);
            const paintingId = c.paintingId || '';
            const title = escapeHtml(c.paintingTitle || 'Без названия');
            const author = escapeHtml(c.paintingAuthor || '');
            const date = c.createdAt
                ? new Date(c.createdAt).toLocaleString('ru-RU', {
                      day: '2-digit',
                      month: 'short',
                      year: 'numeric',
                      hour: '2-digit',
                      minute: '2-digit',
                  })
                : '';
            return `
        <article class="my-comment-card" data-comment-id="${escapeHtml(commentId)}">
            <div class="my-comment-card-head">
                <button type="button" class="my-comment-painting-link"
                    data-painting-id="${escapeHtml(paintingId)}"
                    data-painting-title="${escapeHtml(c.paintingTitle || '')}"
                    data-painting-author="${escapeHtml(c.paintingAuthor || '')}"
                    onclick="openDiscussionFromProfile(this)">${title}</button>
                ${author ? `<span class="my-comment-painting-meta">${author}</span>` : ''}
            </div>
            <p class="my-comment-text">${escapeHtml(c.content)}</p>
            <div class="my-comment-card-foot">
                <time class="my-comment-date">${escapeHtml(date)}</time>
                <button type="button" class="btn-comment-delete" onclick="deleteMyComment('${escapeHtml(commentId)}')">Удалить</button>
            </div>
        </article>`;
        })
        .join('');
}

function openDiscussionFromProfile(btn) {
    selectedPaintingId = btn.dataset.paintingId;
    selectedPaintingTitle = btn.dataset.paintingTitle || '';
    selectedPaintingAuthor = btn.dataset.paintingAuthor || '';
    discussionReturnToProfile = true;
    document.getElementById('discussionTitle').innerText = `Обсуждение: ${selectedPaintingTitle}`;
    document.getElementById('discussionSubtitle').innerText = selectedPaintingAuthor
        ? `Автор: ${selectedPaintingAuthor}`
        : '';
    document.getElementById('commentInput').value = '';
    document.getElementById('commentHint').innerText = '';
    document.getElementById('paintingModal').style.display = 'none';
    document.getElementById('discussionModal').style.display = 'flex';
    loadComments();
}

async function deleteMyComment(commentId) {
    const token = localStorage.getItem('jwt_token');
    if (!token) {
        showToast('Войдите в аккаунт', 'error');
        return;
    }
    if (!confirm('Удалить этот комментарий?')) return;
    try {
        const response = await fetch(`${API_URL}/users/comments/${commentId}`, {
            method: 'DELETE',
            headers: { Authorization: `Bearer ${token.replace(/['"]+/g, '')}` },
        });
        if (response.status === 204 || response.ok) {
            showToast('Комментарий удалён', 'success');
            fetchMyComments();
            return;
        }
        const text = await response.text();
        showToast(text || 'Не удалось удалить', 'error');
    } catch (e) {
        showToast('Сетевая ошибка', 'error');
    }
}

async function submitComment() {
    const token = localStorage.getItem('jwt_token');
    const input = document.getElementById('commentInput');
    const hint = document.getElementById('commentHint');
    const btn = document.getElementById('commentSubmitBtn');
    const content = input.value.trim();

    if (!token) {
        showToast('Войдите, чтобы оставить комментарий', 'error');
        openAuthModal('login');
        return;
    }
    if (!content) {
        hint.innerText = 'Введите текст комментария';
        return;
    }
    hint.innerText = '';

    try {
        setButtonLoading(btn, true);
        const response = await fetch(`${API_URL}/paintings/${selectedPaintingId}/comments`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${token.replace(/['"]+/g, '')}`,
            },
            body: JSON.stringify({ content }),
        });
        const text = await response.text();
        if (!response.ok) {
            showToast(text || 'Не удалось отправить комментарий', 'error');
            setButtonLoading(btn, false, 'Отправить');
            return;
        }
        input.value = '';
        showToast('Комментарий добавлен', 'success');
        setButtonLoading(btn, false, 'Отправить');
        await loadComments();
    } catch (e) {
        showToast('Сетевая ошибка', 'error');
        setButtonLoading(btn, false, 'Отправить');
    }
}

async function sendMessage() {
    if (chatSending) return;
    const input = document.getElementById('chatInput');
    const msg = input.value.trim();
    if (!msg) return;

    const chatBox = document.getElementById('chatBox');
    chatBox.innerHTML += `<div class="message user-message">${escapeHtml(msg)}</div>`;
    input.value = '';

    const typingEl = document.createElement('div');
    typingEl.className = 'message ai-message typing-indicator';
    typingEl.innerHTML = '<span class="typing-dots"><span></span><span></span><span></span></span> Печатает…';
    chatBox.appendChild(typingEl);
    chatBox.scrollTop = chatBox.scrollHeight;

    chatSending = true;
    const context = `«${selectedPaintingTitle}», автор: ${selectedPaintingAuthor}`;

    try {
        const response = await fetch(`${API_URL}/ai/chat`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: msg, context }),
        });
        typingEl.remove();
        const text = await response.text();
        if (!response.ok) {
            chatBox.innerHTML += `<div class="message ai-message error-msg">${escapeHtml(text || 'Ошибка сервера')}</div>`;
            showToast(text || 'ИИ временно недоступен', 'error');
        } else {
            const data = JSON.parse(text);
            const reply = data.reply || data.message || text;
            chatBox.innerHTML += `<div class="message ai-message">${escapeHtml(reply)}</div>`;
        }
    } catch (e) {
        typingEl.remove();
        chatBox.innerHTML += `<div class="message ai-message error-msg">Не удалось связаться с сервером.</div>`;
        showToast('Сетевая ошибка', 'error');
    } finally {
        chatSending = false;
        chatBox.scrollTop = chatBox.scrollHeight;
    }
}
