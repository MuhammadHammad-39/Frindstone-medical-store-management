const state = { token: null, user: null, view: "dashboard", medicines: [], suppliers: [], cart: [], searchTimer: null };
const $ = (selector) => document.querySelector(selector);
const escapeHtml = (value) => String(value ?? "").replace(/[&<>"']/g, (character) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" })[character]);
const money = (value) => new Intl.NumberFormat("en-US", { style: "currency", currency: "USD" }).format(Number(value || 0));
const isAdmin = () => state.user?.role === "ADMIN";
const isExpired = (date) => {
    const now = new Date();
    const localToday = new Date(now.getTime() - now.getTimezoneOffset() * 60000).toISOString().slice(0, 10);
    return date < localToday;
};

async function api(path, options = {}) {
    const response = await fetch(`/api${path}`, {
        ...options,
        headers: { "Content-Type": "application/json", ...(state.token ? { Authorization: `Bearer ${state.token}` } : {}), ...(options.headers || {}) }
    });
    const data = response.status === 204 ? null : await response.json();
    if (!response.ok) throw new Error(data?.error || "The request could not be completed.");
    return data;
}

function showToast(message) {
    $("#toast-message").textContent = message;
    bootstrap.Toast.getOrCreateInstance($("#app-toast"), { delay: 2600 }).show();
}

function navItems() {
    const items = [
        ["dashboard", "Overview", "◫"],
        ["medicines", "Medicines", "＋"],
        ["low-stock", "Stock alerts", "⌁"],
        ["expiry", "Expiry watch", "◷"]
    ];
    if (isAdmin()) items.push(["sales", "Sales report", "▤"], ["suppliers", "Suppliers", "⌂"], ["staff", "Staff", "♙"]);
    return items;
}

function renderNavigation() {
    $("#main-nav").innerHTML = navItems().map(([id, label, icon]) =>
        `<a class="side-link ${state.view === id ? "active" : ""}" href="#${id}" data-view="${id}"><span class="nav-icon" aria-hidden="true">${icon}</span>${label}</a>`
    ).join("");
}

async function openView(view) {
    const allowed = navItems().some(([id]) => id === view);
    state.view = allowed ? view : "dashboard";
    renderNavigation();
    const labels = Object.fromEntries(navItems().map(([id, label]) => [id, label]));
    $("#page-title").textContent = labels[state.view] || "Overview";
    $("#current-section").textContent = (labels[state.view] || "Overview").toUpperCase();
    $("#alert-region").innerHTML = "";
    try {
        if (state.view === "dashboard") await renderDashboard();
        else if (state.view === "medicines") await renderMedicines();
        else if (state.view === "low-stock") await renderMedicineReport("low-stock");
        else if (state.view === "expiry") await renderMedicineReport("expiry");
        else if (state.view === "sales") await renderSales();
        else if (state.view === "suppliers") await renderSuppliers();
        else if (state.view === "staff") await renderStaff();
    } catch (error) { showError(error); }
}

function heading(title, description, button = "") {
    return `<div class="page-heading"><div><h3>${title}</h3><p>${description}</p></div>${button}</div>`;
}

function medicineRows(medicines, actions = false) {
    if (!medicines.length) return `<tr><td colspan="${actions ? 7 : 6}" class="empty-state">No medicines found.</td></tr>`;
    return medicines.map((medicine) => {
        const expired = isExpired(medicine.expiry_date);
        const stockState = Number(medicine.stock) <= Number(medicine.low_stock_threshold) ? "warn" : "";
        const actionsHtml = isAdmin()
            ? `<div class="table-actions"><button class="icon-action" data-action="edit-medicine" data-id="${medicine.id}">Edit</button><button class="icon-action delete" data-action="delete-medicine" data-id="${medicine.id}">Delete</button></div>`
            : `<button class="icon-action" data-action="add-cart" data-id="${medicine.id}" ${expired || Number(medicine.stock) < 1 ? "disabled" : ""}>Add to bill</button>`;
        return `<tr>
            <td><span class="medicine-name">${escapeHtml(medicine.name)}</span><span class="subline">${escapeHtml(medicine.manufacturer)}</span></td>
            <td>${escapeHtml(medicine.category)}</td><td>${money(medicine.price)}</td>
            <td><span class="pill ${stockState}">${medicine.stock} in stock</span></td>
            <td><span class="pill ${expired ? "danger" : "neutral"}">${escapeHtml(medicine.expiry_date)}</span></td>
            <td>${Number(medicine.requires_prescription) ? '<span class="pill warn">Prescription</span>' : '<span class="pill neutral">General</span>'}</td>
            ${actions ? `<td>${actionsHtml}</td>` : ""}
        </tr>`;
    }).join("");
}

function medicineTable(medicines, actions = false) {
    return `<div class="panel"><div class="table-wrap"><table class="table"><thead><tr><th>Medicine</th><th>Category</th><th>Price</th><th>Stock</th><th>Expiry</th><th>Sale type</th>${actions ? "<th class='text-end'>Actions</th>" : ""}</tr></thead><tbody>${medicineRows(medicines, actions)}</tbody></table></div></div>`;
}

async function renderDashboard() {
    const [metrics, stock, expiry] = await Promise.all([api("/dashboard"), api("/reports/low-stock"), api("/reports/expiry")]);
    const lowItems = stock.medicines || [];
    const expiryItems = expiry.medicines || [];
    $("#view-content").innerHTML = `${heading(`Good ${new Date().getHours() < 12 ? "morning" : "afternoon"}, ${escapeHtml(state.user.fullName.split(" ")[0])}`, "A quick read on the store today.")}
        <div class="metric-grid">
            <div class="metric"><div class="metric-label">TOTAL PRODUCTS</div><div class="metric-value">${metrics.totalProducts || 0}</div><div class="metric-foot">Active items in inventory</div></div>
            <div class="metric warning"><div class="metric-label">LOW STOCK</div><div class="metric-value">${metrics.lowStockCount || 0}</div><div class="metric-foot">At or below restock level</div></div>
            <div class="metric danger"><div class="metric-label">EXPIRY WATCH</div><div class="metric-value">${metrics.nearExpiryCount || 0}</div><div class="metric-foot">Expired or due in 30 days</div></div>
            <div class="metric"><div class="metric-label">TODAY'S SALES</div><div class="metric-value">${money(metrics.todaySalesTotal)}</div><div class="metric-foot">Recorded since midnight</div></div>
        </div>
        <div class="dashboard-grid">
            <section class="panel"><div class="panel-heading"><h4>Stock to review</h4><a href="#low-stock" data-view="low-stock">All alerts →</a></div><div class="table-wrap"><table class="table"><thead><tr><th>Medicine</th><th>On hand</th><th>Threshold</th></tr></thead><tbody>${lowItems.length ? lowItems.slice(0, 5).map((item) => `<tr><td><span class="medicine-name">${escapeHtml(item.name)}</span><span class="subline">${escapeHtml(item.category)}</span></td><td><span class="pill warn">${item.stock}</span></td><td>${item.low_stock_threshold}</td></tr>`).join("") : '<tr><td colspan="3" class="empty-state">Stock levels look good.</td></tr>'}</tbody></table></div></section>
            <section class="panel"><div class="panel-heading"><h4>Expiry watch</h4><a href="#expiry" data-view="expiry">Review list →</a></div><div class="panel-body">${expiryItems.length ? expiryItems.slice(0, 5).map((item) => `<div class="cart-item" style="grid-template-columns:1fr auto"><div><strong>${escapeHtml(item.name)}</strong><small>${escapeHtml(item.manufacturer)}</small></div><span class="pill ${isExpired(item.expiry_date) ? "danger" : "warn"}">${escapeHtml(item.expiry_date)}</span></div>`).join("") : '<div class="empty-state">No upcoming expirations.</div>'}</div></section>
        </div>`;
}

async function renderMedicines(filter = "") {
    const queryType = $("#medicine-search-type")?.value || "name";
    let url = "/medicines";
    if (filter.trim()) url += `?${queryType}=${encodeURIComponent(filter.trim())}`;
    state.medicines = await api(url);
    const addButton = isAdmin() ? '<button class="btn btn-dark" data-action="new-medicine">＋ Add medicine</button>' : "";
    const search = `<div class="search-tools"><select id="medicine-search-type" class="form-select" aria-label="Search by"><option value="name">Name</option><option value="category">Category</option><option value="id">ID</option></select><input id="medicine-search" class="form-control" value="${escapeHtml(filter)}" placeholder="Search inventory" aria-label="Search inventory"><button class="btn btn-outline-secondary" data-action="search-medicines">Search</button></div>`;
    const table = `${medicineTable(state.medicines, true)}`;
    $("#view-content").innerHTML = `${heading("Medicine inventory", isAdmin() ? "Update the catalogue and keep stock records current." : "Search available items and add them to the customer bill.", addButton)}<div class="inventory-layout ${isAdmin() ? "no-cart" : ""}"><div>${search}<div class="mt-3">${table}</div></div>${isAdmin() ? "" : renderCart()}</div>`;
    if (filter && $("#medicine-search-type")) $("#medicine-search-type").value = queryType;
}

function renderCart() {
    const lines = state.cart.map((item, index) => `<div class="cart-item"><div><strong>${escapeHtml(item.name)}</strong><small>${money(item.price)} each</small>${item.requiresPrescription ? `<input class="prescription-reference mt-1" data-index="${index}" placeholder="Prescription ref. *" value="${escapeHtml(item.prescriptionReference)}">` : ""}</div><input class="cart-quantity" data-index="${index}" type="number" min="1" max="${item.stock}" value="${item.quantity}" aria-label="Quantity"><button class="cart-remove" data-action="remove-cart" data-index="${index}" aria-label="Remove">×</button></div>`).join("");
    const total = state.cart.reduce((sum, item) => sum + item.price * item.quantity, 0);
    return `<section class="panel cart-panel"><div class="panel-heading"><h4>Current bill</h4><span class="pill neutral">${state.cart.length} lines</span></div><div class="panel-body">${lines || '<div class="cart-empty">Add medicines from the inventory to begin.</div>'}<div class="cart-total"><span>Subtotal</span><span>${money(total)}</span></div><button class="btn btn-dark w-100" data-action="checkout" ${state.cart.length ? "" : "disabled"}>Complete sale</button></div></section>`;
}

async function renderMedicineReport(type) {
    const report = await api(`/reports/${type}`);
    const title = type === "expiry" ? "Expiry watch" : "Low-stock alerts";
    const description = type === "expiry" ? "Products expiring within 30 days, including expired stock." : "Products at or below their configured restock level.";
    const products = report.medicines || [];
    $("#view-content").innerHTML = `${heading(title, description)}<div class="panel-heading panel mb-3"><h4>${products.length} ${products.length === 1 ? "item" : "items"} flagged</h4><span class="pill ${type === "expiry" ? "warn" : "danger"}">${type === "expiry" ? "30-day window" : "Restock"}</span></div>${medicineTable(products)}`;
}

async function renderSales() {
    const report = await api("/reports/sales");
    $("#view-content").innerHTML = `${heading("Sales report", "Recent bills and cumulative recorded sales.")}<div class="panel p-3 mb-3"><div class="report-total">ALL RECORDED SALES<br><strong>${money(report.total)}</strong></div></div><section class="panel"><div class="panel-heading"><h4>Recent transactions</h4><span class="pill neutral">Last 100</span></div><div class="table-wrap"><table class="table"><thead><tr><th>Receipt</th><th>Cashier</th><th>Date</th><th class="text-end">Total</th></tr></thead><tbody>${report.sales.length ? report.sales.map((sale) => `<tr><td class="medicine-name">#${sale.id}</td><td>${escapeHtml(sale.cashier)}</td><td>${escapeHtml(sale.createdAt)}</td><td class="text-end">${money(sale.total)}</td></tr>`).join("") : '<tr><td colspan="4" class="empty-state">No sales recorded yet.</td></tr>'}</tbody></table></div></section>`;
}

async function renderSuppliers() {
    state.suppliers = await api("/admin/suppliers");
    $("#view-content").innerHTML = `${heading("Suppliers", "Maintain supplier contacts linked to your inventory.", '<button class="btn btn-dark" data-action="new-supplier">＋ Add supplier</button>')}<section class="panel"><div class="table-wrap"><table class="table"><thead><tr><th>Supplier</th><th>Phone</th><th>Email</th><th class="text-end">Actions</th></tr></thead><tbody>${state.suppliers.length ? state.suppliers.map((supplier) => `<tr><td class="medicine-name">${escapeHtml(supplier.name)}</td><td>${escapeHtml(supplier.phone)}</td><td>${escapeHtml(supplier.email)}</td><td><div class="table-actions"><button class="icon-action" data-action="edit-supplier" data-id="${supplier.id}">Edit</button><button class="icon-action delete" data-action="delete-supplier" data-id="${supplier.id}">Delete</button></div></td></tr>`).join("") : '<tr><td colspan="4" class="empty-state">No suppliers on file.</td></tr>'}</tbody></table></div></section>`;
}

async function renderStaff() {
    const staff = await api("/admin/staff");
    $("#view-content").innerHTML = `${heading("Staff directory", "Accounts and access roles for this store.")}<section class="panel"><div class="table-wrap"><table class="table"><thead><tr><th>Team member</th><th>Username</th><th>Role</th><th>Access</th></tr></thead><tbody>${staff.map((member) => `<tr><td class="medicine-name">${escapeHtml(member.fullName)}</td><td>${escapeHtml(member.username)}</td><td><span class="pill neutral">${escapeHtml(member.role)}</span></td><td>${member.role === "ADMIN" ? "Full access" : "Sales & inventory"}</td></tr>`).join("")}</tbody></table></div></section>`;
}

function showError(error) {
    if (error.message.includes("sign in") || error.message.includes("Session expired")) return signOut();
    $("#alert-region").innerHTML = `<div class="inline-alert">${escapeHtml(error.message)}</div>`;
}

function showMedicineModal(medicine = null) {
    const form = $("#medicine-form"); form.reset();
    $("#medicine-id").value = medicine?.id || "";
    $("#medicine-modal-title").textContent = medicine ? "Edit medicine" : "Add medicine";
    $("#medicine-name").value = medicine?.name || "";
    $("#medicine-category").value = medicine?.category || "";
    $("#medicine-manufacturer").value = medicine?.manufacturer || "";
    $("#medicine-price").value = medicine?.price ?? "";
    $("#medicine-stock").value = medicine?.stock ?? "";
    $("#medicine-threshold").value = medicine?.low_stock_threshold ?? 10;
    $("#medicine-expiry").value = medicine?.expiry_date || "";
    $("#medicine-prescription").checked = Number(medicine?.requires_prescription || 0) === 1;
    const suppliers = isAdmin() ? state.suppliers : [];
    $("#medicine-supplier").innerHTML = `<option value="">No supplier</option>${suppliers.map((supplier) => `<option value="${supplier.id}">${escapeHtml(supplier.name)}</option>`).join("")}`;
    $("#medicine-supplier").value = medicine?.supplier_id || "";
    bootstrap.Modal.getOrCreateInstance($("#medicine-modal")).show();
}

function showSupplierModal(supplier = null) {
    $("#supplier-form").reset();
    $("#supplier-id").value = supplier?.id || "";
    $("#supplier-modal-title").textContent = supplier ? "Edit supplier" : "Add supplier";
    $("#supplier-name").value = supplier?.name || "";
    $("#supplier-phone").value = supplier?.phone || "";
    $("#supplier-email").value = supplier?.email || "";
    bootstrap.Modal.getOrCreateInstance($("#supplier-modal")).show();
}

async function handleAction(button) {
    const id = Number(button.dataset.id);
    if (button.dataset.action === "new-medicine") {
        if (!state.suppliers.length) state.suppliers = await api("/admin/suppliers");
        showMedicineModal();
    } else if (button.dataset.action === "edit-medicine") {
        const medicine = state.medicines.find((item) => Number(item.id) === id);
        if (!state.suppliers.length) state.suppliers = await api("/admin/suppliers");
        showMedicineModal(medicine);
    } else if (button.dataset.action === "delete-medicine") {
        if (confirm("Delete this medicine from the catalogue?")) { await api(`/admin/medicines/${id}`, { method: "DELETE" }); showToast("Medicine deleted"); await renderMedicines(); }
    } else if (button.dataset.action === "new-supplier") showSupplierModal();
    else if (button.dataset.action === "edit-supplier") showSupplierModal(state.suppliers.find((item) => Number(item.id) === id));
    else if (button.dataset.action === "delete-supplier") {
        if (confirm("Delete this supplier?")) { await api(`/admin/suppliers/${id}`, { method: "DELETE" }); showToast("Supplier deleted"); await renderSuppliers(); }
    } else if (button.dataset.action === "search-medicines") await renderMedicines($("#medicine-search")?.value || "");
    else if (button.dataset.action === "add-cart") {
        const medicine = state.medicines.find((item) => Number(item.id) === id);
        const existing = state.cart.find((item) => Number(item.id) === id);
        if (existing) existing.quantity = Math.min(existing.quantity + 1, Number(medicine.stock));
        else state.cart.push({ id: medicine.id, name: medicine.name, price: Number(medicine.price), stock: Number(medicine.stock), requiresPrescription: Number(medicine.requires_prescription) === 1, prescriptionReference: "", quantity: 1 });
        await renderMedicines($("#medicine-search")?.value || "");
    } else if (button.dataset.action === "remove-cart") {
        state.cart.splice(Number(button.dataset.index), 1); await renderMedicines($("#medicine-search")?.value || "");
    } else if (button.dataset.action === "checkout") await checkout();
}

async function checkout() {
    const items = state.cart.map((item) => ({ medicineId: item.id, quantity: item.quantity, prescriptionReference: item.prescriptionReference }));
    try {
        const receipt = await api("/checkout", { method: "POST", body: JSON.stringify({ items }) });
        state.cart = [];
        $("#receipt-content").innerHTML = `<div class="receipt-head"><strong>Fieldstone Pharmacy</strong><small>Receipt #${receipt.id} · ${escapeHtml(receipt.createdAt)} · Served by ${escapeHtml(receipt.cashier)}</small></div><div class="receipt-lines">${receipt.items.map((item) => `<div class="receipt-line"><span>${escapeHtml(item.name)}<small>${item.quantity} × ${money(item.unitPrice)}</small></span><strong>${money(item.lineTotal)}</strong></div>`).join("")}</div><div class="receipt-grand"><span>Total paid</span><span>${money(receipt.total)}</span></div>`;
        bootstrap.Modal.getOrCreateInstance($("#receipt-modal")).show();
        await renderMedicines();
    } catch (error) { showError(error); }
}

function signOut() {
    state.token = null; state.user = null; state.cart = [];
    sessionStorage.removeItem("medical-store-user");
    $("#application").classList.add("d-none"); $("#login-screen").classList.remove("d-none");
}

function enterApp(user, token) {
    state.user = user; state.token = token;
    sessionStorage.setItem("medical-store-user", JSON.stringify({ user, token }));
    $("#login-screen").classList.add("d-none"); $("#application").classList.remove("d-none");
    $("#user-name").textContent = user.fullName; $("#user-role").textContent = user.role.toLowerCase();
    $("#user-avatar").textContent = user.fullName.charAt(0).toUpperCase();
    $("#today-label").textContent = new Intl.DateTimeFormat("en-US", { weekday: "short", month: "short", day: "numeric" }).format(new Date());
    openView("dashboard");
}

document.addEventListener("submit", async (event) => {
    event.preventDefault();
    try {
        if (event.target.id === "login-form") {
            $("#login-error").textContent = "";
            const result = await api("/auth/login", { method: "POST", body: JSON.stringify({ username: $("#username-input").value, password: $("#password-input").value, role: $("#role-input").value }) });
            const { token, ...user } = result; enterApp(user, token);
        } else if (event.target.id === "medicine-form") {
            const id = $("#medicine-id").value;
            const body = { name: $("#medicine-name").value, category: $("#medicine-category").value, manufacturer: $("#medicine-manufacturer").value, price: Number($("#medicine-price").value), stock: Number($("#medicine-stock").value), lowStockThreshold: Number($("#medicine-threshold").value), expiryDate: $("#medicine-expiry").value, requiresPrescription: $("#medicine-prescription").checked, supplierId: $("#medicine-supplier").value || null };
            await api(`/admin/medicines${id ? `/${id}` : ""}`, { method: id ? "PUT" : "POST", body: JSON.stringify(body) });
            bootstrap.Modal.getInstance($("#medicine-modal")).hide(); showToast(id ? "Medicine updated" : "Medicine added"); await renderMedicines();
        } else if (event.target.id === "supplier-form") {
            const id = $("#supplier-id").value;
            const body = { name: $("#supplier-name").value, phone: $("#supplier-phone").value, email: $("#supplier-email").value };
            await api(`/admin/suppliers${id ? `/${id}` : ""}`, { method: id ? "PUT" : "POST", body: JSON.stringify(body) });
            bootstrap.Modal.getInstance($("#supplier-modal")).hide(); showToast(id ? "Supplier updated" : "Supplier added"); await renderSuppliers();
        }
    } catch (error) {
        if (event.target.id === "login-form") $("#login-error").textContent = error.message;
        else showError(error);
    }
});

document.addEventListener("click", async (event) => {
    const nav = event.target.closest("[data-view]");
    if (nav) { event.preventDefault(); await openView(nav.dataset.view); return; }
    const button = event.target.closest("[data-action]");
    if (button) { try { await handleAction(button); } catch (error) { showError(error); } }
});

document.addEventListener("input", (event) => {
    if (event.target.classList.contains("cart-quantity")) {
        const item = state.cart[Number(event.target.dataset.index)];
        item.quantity = Math.max(1, Math.min(Number(event.target.value) || 1, item.stock));
        const panel = $(".cart-panel"); if (panel) panel.outerHTML = renderCart();
    } else if (event.target.classList.contains("prescription-reference")) {
        state.cart[Number(event.target.dataset.index)].prescriptionReference = event.target.value;
    }
});

document.addEventListener("change", (event) => {
    if (event.target.id === "role-input") {
        const admin = event.target.value === "ADMIN";
        $("#username-input").value = admin ? "admin" : "pharmacist";
        $("#password-input").value = admin ? "admin123" : "pharm123";
    }
    if (event.target.id === "medicine-search-type" && $("#medicine-search")?.value) renderMedicines($("#medicine-search").value).catch(showError);
});

$("#logout-button").addEventListener("click", async () => {
    try { await api("/auth/logout", { method: "POST" }); } catch { /* Local sign-out still clears the client session. */ }
    signOut();
});
$("#print-receipt").addEventListener("click", () => window.print());

const saved = sessionStorage.getItem("medical-store-user");
if (saved) {
    try { const session = JSON.parse(saved); enterApp(session.user, session.token); }
    catch { sessionStorage.removeItem("medical-store-user"); }
}