const reportMsg = document.getElementById("report-msg");

function renderLineItems(map) {
  const entries = Object.entries(map || {});
  if (entries.length === 0) return `<p class="muted">No entries.</p>`;
  return `<table><tbody>${entries.map(([name, amt]) => `
    <tr><td>${name}</td><td class="num">${money(amt)}</td></tr>
  `).join("")}</tbody></table>`;
}

async function loadProfitLoss() {
  const body = document.getElementById("pl-body");
  const year = document.getElementById("plYear").value;
  body.innerHTML = "Loading…";
  try {
    const url = year ? `/api/reports/profit-loss?year=${year}` : "/api/reports/profit-loss";
    const pl = await apiFetch(url);
    body.innerHTML = `
      <h3 style="font-size:13px; color:var(--color-muted); margin: 10px 0 4px;">Revenue</h3>
      ${renderLineItems(pl.revenueByAccount)}
      <p><strong>Total revenue:</strong> <span class="mono">${money(pl.totalRevenue)}</span></p>
      <h3 style="font-size:13px; color:var(--color-muted); margin: 10px 0 4px;">Expenses</h3>
      ${renderLineItems(pl.expensesByAccount)}
      <p><strong>Total expenses:</strong> <span class="mono">${money(pl.totalExpenses)}</span></p>
      <p style="font-size:16px;"><strong>Net profit:</strong> <span class="mono">${money(pl.netProfit)}</span></p>
    `;
  } catch (err) {
    showError(reportMsg, err);
  }
}

async function loadBalanceSheet() {
  const body = document.getElementById("bs-body");
  body.innerHTML = "Loading…";
  try {
    const bs = await apiFetch("/api/reports/balance-sheet");
    body.innerHTML = `
      <h3 style="font-size:13px; color:var(--color-muted); margin: 10px 0 4px;">Assets</h3>
      ${renderLineItems(bs.assets)}
      <p><strong>Total assets:</strong> <span class="mono">${money(bs.totalAssets)}</span></p>
      <h3 style="font-size:13px; color:var(--color-muted); margin: 10px 0 4px;">Liabilities</h3>
      ${renderLineItems(bs.liabilities)}
      <p><strong>Total liabilities:</strong> <span class="mono">${money(bs.totalLiabilities)}</span></p>
      <h3 style="font-size:13px; color:var(--color-muted); margin: 10px 0 4px;">Equity</h3>
      ${renderLineItems(bs.equity)}
      <p style="font-size:16px;"><strong>Total equity:</strong> <span class="mono">${money(bs.totalEquity)}</span></p>
    `;
  } catch (err) {
    showError(reportMsg, err);
  }
}

async function downloadCsv(path, filename) {
  try {
    const csvText = await apiFetch(path);
    const blob = new Blob([csvText], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    showError(reportMsg, err);
  }
}

document.getElementById("pl-load").addEventListener("click", loadProfitLoss);
document.getElementById("bs-load").addEventListener("click", loadBalanceSheet);

document.getElementById("pl-csv-link").addEventListener("click", (e) => {
  e.preventDefault();
  const year = document.getElementById("plYear").value;
  const url = year ? `/api/reports/profit-loss/csv?year=${year}` : "/api/reports/profit-loss/csv";
  downloadCsv(url, "profit_and_loss.csv");
});

document.getElementById("bs-csv-link").addEventListener("click", (e) => {
  e.preventDefault();
  downloadCsv("/api/reports/balance-sheet/csv", "balance_sheet.csv");
});
