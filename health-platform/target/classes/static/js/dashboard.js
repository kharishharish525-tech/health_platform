document.getElementById("today-label").textContent = new Date().toLocaleDateString(undefined, {
  weekday: "long", year: "numeric", month: "long", day: "numeric"
});

async function loadDashboard() {
  const errorBox = document.getElementById("error-box");
  try {
    const summary = await apiFetch("/api/dashboard/summary");
    document.getElementById("stat-patients").textContent = summary.totalPatients;
    document.getElementById("stat-doctors").textContent = summary.totalDoctors;
    document.getElementById("stat-waiting").textContent = summary.waitingQueueCount;
    document.getElementById("stat-emergency").textContent = summary.emergencyWaitingCount;
    document.getElementById("stat-consults").textContent = summary.todaysConsultations;
    document.getElementById("stat-revenue").textContent = money(summary.todaysRevenue);
    document.getElementById("stat-unpaid").textContent = money(summary.totalUnpaidAmount);
    document.getElementById("stat-lowstock").textContent = summary.lowStockProductCount;
  } catch (err) {
    showError(errorBox, err);
  }

  try {
    const queue = await apiFetch("/api/triage/queue");
    const tbody = document.querySelector("#queue-table tbody");
    if (queue.length === 0) {
      tbody.innerHTML = `<tr><td colspan="5" class="muted">No patients waiting.</td></tr>`;
      return;
    }
    tbody.innerHTML = queue.slice(0, 8).map(c => `
      <tr>
        <td>${c.patient ? c.patient.fullName : "—"}</td>
        <td>${priorityBadge(c.triagePriority)}</td>
        <td class="num">${c.triageScore ?? ""}</td>
        <td>${(c.symptoms || "").slice(0, 60)}</td>
        <td>${formatDateTime(c.createdAt)}</td>
      </tr>
    `).join("");
  } catch (err) {
    showError(errorBox, err);
  }
}

loadDashboard();
