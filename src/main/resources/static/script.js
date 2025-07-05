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

    submitBtn.disabled = true;
    submitBtn.textContent = "Processing...";
    spinner.style.display = "block";
    logContainerWrapper.style.display = "none";
    logContainer.innerHTML = ""; // Use innerHTML to clear content

    try {
      const response = await fetch("/api/workflow/start", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ resourceDetails: resourceDetails }),
      });

      const result = await response.json();
      spinner.style.display = "none";

      if (response.ok && result.log) {
        logContainerWrapper.style.display = "block";
        await renderLog(result.log);
      } else {
        logContainerWrapper.style.display = "block";
        const errorLog = result.log || [
          {
            type: "ERROR",
            title: "Unknown Error",
            details: "An unknown error occurred.",
          },
        ];
        await renderLog(errorLog);
      }
    } catch (error) {
      spinner.style.display = "none";
      logContainerWrapper.style.display = "block";
      const errorLog = [
        {
          type: "ERROR",
          title: "Connection Error",
          details:
            "Failed to connect to the server. Please ensure the backend is running.",
        },
      ];
      await renderLog(errorLog);
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Start Workflow";
    }
  });

function renderLog(logEntries) {
  const logContainer = document.getElementById("log-container");
  const icons = {
    USER_INPUT: "👤",
    AI_DECISION: "🧠",
    AI_ACTION: "🤖",
    SYSTEM_ACTION: "⚙️",
    FINAL_STATUS: "🏁",
    ERROR: "⚠️",
  };

  // Use a promise to handle the sequential rendering with delays
  return new Promise((resolve) => {
    let delay = 0;
    logEntries.forEach((entry, index) => {
      setTimeout(() => {
        const stepDiv = document.createElement("div");
        stepDiv.className = `log-step type-${entry.type.toLowerCase()}`;

        const icon = icons[entry.type] || "➡️";

        stepDiv.innerHTML = `
                  <div class="log-icon">${icon}</div>
                  <div class="log-content">
                      <h4>${entry.title}</h4>
                      <p>${entry.details.replace(/\n/g, "<br>")}</p>
                  </div>
              `;
        logContainer.appendChild(stepDiv);

        if (index === logEntries.length - 1) {
          resolve();
        }
      }, delay);
      delay += 400; // Add a 400ms delay between each step appearing
    });
  });
}
