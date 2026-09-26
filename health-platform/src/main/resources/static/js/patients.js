const msgBox = document.getElementById("form-msg");

async function loadPatients() {
  const tbody = document.querySelector("#patients-table tbody");
  try {
    const patients = await apiFetch("/api/patients");
    if (patients.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="muted">No patients registered yet.</td></tr>`;
      return;
    }
    tbody.innerHTML = patients.map(p => `
      <tr>
        <td class="num">${p.patientId}</td>
        <td>${p.fullName}</td>
        <td>${p.dateOfBirth || "—"}</td>
        <td>${p.gender || "—"}</td>
        <td>${p.phone || "—"}</td>
        <td>${p.bloodGroup || "—"}</td>
      </tr>
    `).join("");
  } catch (err) {
    showError(msgBox, err);
  }
}

document.getElementById("patient-form").addEventListener("submit", async (e) => {
  e.preventDefault();
  const payload = {
    fullName: document.getElementById("fullName").value,
    dateOfBirth: document.getElementById("dateOfBirth").value || null,
    gender: document.getElementById("gender").value || null,
    phone: document.getElementById("phone").value || null,
    bloodGroup: document.getElementById("bloodGroup").value || null,
    emergencyContact: document.getElementById("emergencyContact").value || null,
    address: document.getElementById("address").value || null
  };
  try {
    await apiFetch("/api/patients", { method: "POST", body: JSON.stringify(payload) });
    showSuccess(msgBox, "Patient registered.");
    document.getElementById("patient-form").reset();
    loadPatients();
  } catch (err) {
    showError(msgBox, err);
  }
});

loadPatients();
