const invoiceMsg = document.getElementById("invoice-msg");
const paymentMsg = document.getElementById("payment-msg");
const vendorBillMsg = document.getElementById("vendor-bill-msg");

async function loadDropdowns() {
  try {
    const [patients, products, suppliers] = await Promise.all([
      apiFetch("/api/patients"),
      apiFetch("/api/products"),
      apiFetch("/api/suppliers")
    ]);
    document.getElementById("invPatient").innerHTML = patients.length
      ? patients.map(p => `<option value="${p.patientId}">${p.fullName} (#${p.patientId})</option>`).join("")
      : `<option value="">No patients yet</option>`;

    document.getElementById("invProduct").innerHTML = products.length
      ? products.map(p => `<option value="${p.productId}">${p.name} — ${money(p.unitPrice)}</option>`).join("")
      : `<option value="">No products yet — add some via /api/products</option>`;

    document.getElementById("vendorSupplier").innerHTML = suppliers.length
      ? suppliers.map(s => `<option value="${s.supplierId}">${s.name}</option>`).join("")
      : `<option value="">No suppliers yet</option>`;
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

async function loadVendorBills() {
  const tbody = document.querySelector("#vendor-bills-table tbody");
  try {
    const bills = await apiFetch("/api/vendor-bills");
    if (bills.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted">No supplier bills yet.</td></tr>`;
      return;
    }
    tbody.innerHTML = bills.map(b => `
      <tr>
        <td class="num">${b.vendorBillId}</td>
        <td>${b.supplier ? b.supplier.name : "—"}</td>
        <td>${b.billNumber}</td>
        <td class="num">${money(b.amount)}</td>
        <td>${statusBadge(b.status)}</td>
      </tr>
    `).join("");
  } catch (err) {
    showError(vendorBillMsg, err);
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

document.getElementById("vendor-bill-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const payload = {
    supplierId: Number(document.getElementById("vendorSupplier").value),
    billNumber: document.getElementById("vendorBillNumber").value,
    amount: Number(document.getElementById("vendorAmount").value),
    notes: document.getElementById("vendorNotes").value || ""
  };
  try {
    await apiFetch("/api/vendor-bills", { method: "POST", body: JSON.stringify(payload) });
    showSuccess(vendorBillMsg, "Vendor bill saved.");
    document.getElementById("vendor-bill-form").reset();
    loadVendorBills();
  } catch (err) {
    showError(vendorBillMsg, err);
  }
});

loadDropdowns();
loadInvoices();
loadVendorBills();
loadLowStock();
