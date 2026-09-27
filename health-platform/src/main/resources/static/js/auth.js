const messageEl = document.getElementById("auth-message");

function showAuthMessage(message) {
  messageEl.textContent = message;
  messageEl.hidden = false;
}

if (window.location.pathname === "/login.html") {
  const params = new URLSearchParams(window.location.search);
  if (params.has("error")) showAuthMessage("Username or password is incorrect.");
  if (params.has("logout")) {
    messageEl.classList.remove("error");
    messageEl.classList.add("success");
    showAuthMessage("You have signed out.");
  }
  if (params.has("registered")) {
    messageEl.classList.remove("error");
    messageEl.classList.add("success");
    showAuthMessage("Account created. Sign in to continue.");
  }
}

const signupForm = document.getElementById("signup-form");
if (signupForm) {
  signupForm.addEventListener("submit", async (event) => {
    event.preventDefault();
    const submitButton = signupForm.querySelector("button[type=submit]");
    submitButton.disabled = true;
    messageEl.hidden = true;
    try {
      const response = await fetch("/api/auth/signup", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          username: signupForm.elements.username.value,
          password: signupForm.elements.password.value
        })
      });
      const result = await response.json();
      if (!response.ok) throw new Error(result.message || "Could not create the account.");
      window.location.href = "/login.html?registered";
    } catch (error) {
      showAuthMessage(error.message || "Could not create the account.");
      submitButton.disabled = false;
    }
  });
}