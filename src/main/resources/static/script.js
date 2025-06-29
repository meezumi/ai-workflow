document
  .getElementById("requisitionForm")
  .addEventListener("submit", async function (event) {
    event.preventDefault();

    const resourceDetails = document.getElementById("resourceDetails").value;
    const submitBtn = document.getElementById("submitBtn");
    const spinner = document.getElementById("spinner");
    const logContainerWrapper = document.getElementById(
      "log-container-wrapper"
    );
    const logContainer = document.getElementById("log-container");

    // UI updates for loading state
    submitBtn.disabled = true;
    submitBtn.textContent = "Processing...";
    spinner.style.display = "block";
    logContainerWrapper.style.display = "none";
    logContainer.textContent = "";

    try {
      const response = await fetch("/api/workflow/start", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ resourceDetails: resourceDetails }),
      });

      const result = await response.json();

      if (response.ok) {
        logContainer.textContent = result.log;
        logContainerWrapper.style.display = "block";
      } else {
        logContainer.textContent = `Error: ${
          result.log || "An unknown error occurred."
        }`;
        logContainerWrapper.style.display = "block";
      }
    } catch (error) {
      console.error("Error:", error);
      logContainer.textContent =
        "Failed to connect to the server. Please ensure the backend is running.";
      logContainerWrapper.style.display = "block";
    } finally {
      // Reset UI
      submitBtn.disabled = false;
      submitBtn.textContent = "Start Workflow";
      spinner.style.display = "none";
    }
  });
