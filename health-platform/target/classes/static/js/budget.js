const budgetMsg = document.getElementById("budget-msg");
const utilMsg = document.getElementById("util-msg");

const now = new Date();
document.getElementById("budgetYear").value = now.getFullYear();
document.getElementById("budgetMonth").value = now.getMonth() + 1;
document.getElementById("utilYear").value = now.getFullYear();
document.getElementById("utilMonth").value = now.getMonth() + 1;

async function loadDepartments() {
  try {
    const departments = await apiFetch("/api/departments");
    document.getElementById("budgetDept").innerHTML = departments.length
      ? departments.map(d => `<option value="${d.departmentId}">${d.name}</option>`).join("")
      : `<option value="">No departments yet — add one via /api/departments</option>`;
  } catch (err) {
    showError(budgetMsg, err);
  }
}

document.getElementById("budget-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const payload = {
    department: { departmentId: Number(document.getElementById("budgetDept").value) },
    fiscalYear: Number(document.getElementById("budgetYear").value),
    fiscalMonth: Number(document.getElementById("budgetMonth").value),
    allocatedAmount: Number(document.getElementById("budgetAmount").value)
  };
  try {
    await apiFetch("/api/budgets", { method: "POST", body: JSON.stringify(payload) });
    showSuccess(budgetMsg, "Budget allocated.");
    document.getElementById("budget-form").reset();
  } catch (err) {
    showError(budgetMsg, err);
  }
});

async function loadUtilization(year, month) {
  const tbody = document.querySelector("#utilization-table tbody");
  try {
    const rows = await apiFetch(`/api/budgets/utilization?year=${year}&month=${month}`);
    if (rows.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted">No budgets allocated for that period.</td></tr>`;
      return;
    }
    tbody.innerHTML = rows.map(r => `
      <tr>
        <td>${r.departmentName}</td>
        <td class="num">${money(r.allocatedAmount)}</td>
        <td class="num">${money(r.spentAmount)}</td>
        <td class="num">${money(r.remainingAmount)}</td>
        <td class="num">${r.utilizationPercent.toFixed(1)}%</td>
      </tr>
    `).join("");
  } catch (err) {
    showError(utilMsg, err);
  }
}

document.getElementById("util-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const year = document.getElementById("utilYear").value;
  const month = document.getElementById("utilMonth").value;
  loadUtilization(year, month);
});

// CSV download needs the Authorization header, so fetch it as a blob rather
// than navigating a plain <a href> (which wouldn't carry Basic auth).
document.getElementById("util-csv-link").addEventListener("click", async (e) => {
  e.preventDefault();
  const year = document.getElementById("utilYear").value;
  const month = document.getElementById("utilMonth").value;
  try {
    const csvText = await apiFetch(`/api/budgets/utilization/csv?year=${year}&month=${month}`);
    const blob = new Blob([csvText], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = `budget_utilization_${year}_${month}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  } catch (err) {
    showError(utilMsg, err);
  }
});

loadDepartments();
