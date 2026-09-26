const triageMsg = document.getElementById("triage-msg");

async function loadDropdowns() {
  try {
    const [patients, departments, doctors] = await Promise.all([
      apiFetch("/api/patients"),
      apiFetch("/api/departments"),
      apiFetch("/api/doctors")
    ]);

    const patientSelect = document.getElementById("patientSelect");
    patientSelect.innerHTML = patients.length
      ? patients.map(p => `<option value="${p.patientId}">${p.fullName} (#${p.patientId})</option>`).join("")
      : `<option value="">No patients — register one first</option>`;

    const deptSelect = document.getElementById("departmentSelect");
    deptSelect.innerHTML = `<option value="">—</option>` +
      departments.map(d => `<option value="${d.departmentId}">${d.name}</option>`).join("");

    const doctorSelect = document.getElementById("doctorSelect");
    doctorSelect.innerHTML = `<option value="">—</option>` +
      doctors.map(d => `<option value="${d.doctorId}">${d.fullName}</option>`).join("");
  } catch (err) {
    showError(triageMsg, err);
  }
}

async function loadQueue() {
  const tbody = document.querySelector("#queue-table tbody");
  try {
    const queue = await apiFetch("/api/triage/queue");
    if (queue.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="muted">No patients waiting.</td></tr>`;
      return;
    }
    tbody.innerHTML = queue.map(c => `
      <tr>
        <td>${c.patient ? c.patient.fullName : "—"}</td>
        <td>${priorityBadge(c.triagePriority)}</td>
        <td class="num">${c.triageScore ?? ""}</td>
        <td>${(c.symptoms || "").slice(0, 70)}</td>
        <td>${formatDateTime(c.createdAt)}</td>
        <td>${c.status}</td>
      </tr>
    `).join("");
  } catch (err) {
    showError(triageMsg, err);
  }
}

document.getElementById("triage-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const num = (id) => {
    const v = document.getElementById(id).value;
    return v === "" ? null : Number(v);
  };
  const payload = {
    patientId: Number(document.getElementById("patientSelect").value),
    departmentId: document.getElementById("departmentSelect").value || null,
    doctorId: document.getElementById("doctorSelect").value || null,
    symptoms: document.getElementById("symptoms").value,
    heartRate: num("heartRate"),
    systolicBp: num("systolicBp"),
    diastolicBp: num("diastolicBp"),
    temperatureCelsius: num("temperatureCelsius"),
    spo2: num("spo2"),
    respiratoryRate: num("respiratoryRate")
  };

  try {
    const result = await apiFetch("/api/triage/evaluate", { method: "POST", body: JSON.stringify(payload) });
    const card = document.getElementById("result-card");
    card.style.display = "block";
    document.getElementById("result-body").innerHTML = `
      <p><strong>Priority:</strong> ${priorityBadge(result.triagePriority)}</p>
      <p><strong>Score:</strong> <span class="mono">${result.triageScore}</span></p>
      <p><strong>Consultation ID:</strong> <span class="mono">#${result.consultationId}</span></p>
      ${result.flaggedReasons && result.flaggedReasons.length
        ? `<p><strong>Flagged:</strong></p><ul>${result.flaggedReasons.map(r => `<li>${r}</li>`).join("")}</ul>`
        : `<p class="muted">No abnormal vitals or red-flag symptoms detected.</p>`}
    `;
    document.getElementById("triage-form").reset();
    loadQueue();
  } catch (err) {
    showError(triageMsg, err);
  }
});

loadDropdowns();
loadQueue();
setInterval(loadQueue, 15000); // auto-refresh the queue every 15s
