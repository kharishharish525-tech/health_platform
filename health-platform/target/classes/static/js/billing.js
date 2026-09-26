const invoiceMsg = document.getElementById("invoice-msg");
const paymentMsg = document.getElementById("payment-msg");

async function loadDropdowns() {
  try {
    const [patients, products] = await Promise.all([
      apiFetch("/api/patients"),
      apiFetch("/api/products")
    ]);
    document.getElementById("invPatient").innerHTML = patients.length
      ? patients.map(p => `<option value="${p.patientId}">${p.fullName} (#${p.patientId})</option>`).join("")
      : `<option value="">No patients yet</option>`;

    document.getElementById("invProduct").innerHTML = products.length
      ? products.map(p => `<option value="${p.productId}">${p.name} — ${money(p.unitPrice)}</option>`).join("")
      : `<option value="">No products yet — add some via /api/products</option>`;
  } catch (err) {
    showError(invoiceMsg, err);
  }
}

async function loadInvoices() {
  const tbody = document.querySelector("#invoices-table tbody");
  try {
    const invoices = await apiFetch("/api/invoices");
    if (invoices.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted">No invoices yet.</td></tr>`;
      return;
    }
    tbody.innerHTML = invoices.map(inv => `
      <tr>
        <td class="num">${inv.invoiceId}</td>
        <td>${inv.patient ? inv.patient.fullName : "—"}</td>
        <td>${formatDateTime(inv.invoiceDate)}</td>
        <td class="num">${money(inv.totalAmount)}</td>
        <td>${statusBadge(inv.status)}</td>
      </tr>
    `).join("");
  } catch (err) {
    showError(invoiceMsg, err);
  }
}

async function loadLowStock() {
  const tbody = document.querySelector("#lowstock-table tbody");
  try {
    const products = await apiFetch("/api/products/low-stock");
    if (products.length === 0) {
      tbody.innerHTML = `<tr><td colspan="3" class="muted">Nothing below reorder level.</td></tr>`;
      return;
    }
    tbody.innerHTML = products.map(p => `
      <tr>
        <td>${p.name}</td>
        <td class="num">${p.stockQuantity}</td>
        <td class="num">${p.reorderLevel}</td>
      </tr>
    `).join("");
  } catch (err) {
    showError(invoiceMsg, err);
  }
}

document.getElementById("invoice-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const payload = {
    patientId: Number(document.getElementById("invPatient").value),
    consultationId: null,
    lines: [{
      productId: Number(document.getElementById("invProduct").value),
      quantity: Number(document.getElementById("invQty").value)
    }]
  };
  try {
    const invoice = await apiFetch("/api/invoices", { method: "POST", body: JSON.stringify(payload) });
    showSuccess(invoiceMsg, `Invoice #${invoice.invoiceId} created — total ${money(invoice.totalAmount)}.`);
    loadInvoices();
    loadLowStock();
  } catch (err) {
    showError(invoiceMsg, err);
  }
});

document.getElementById("payment-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const payload = {
    invoiceId: Number(document.getElementById("payInvoiceId").value),
    amount: Number(document.getElementById("payAmount").value),
    method: document.getElementById("payMethod").value
  };
  try {
    await apiFetch("/api/payments", { method: "POST", body: JSON.stringify(payload) });
    showSuccess(paymentMsg, "Payment recorded.");
    document.getElementById("payment-form").reset();
    loadInvoices();
  } catch (err) {
    showError(paymentMsg, err);
  }
});

loadDropdowns();
loadInvoices();
loadLowStock();
