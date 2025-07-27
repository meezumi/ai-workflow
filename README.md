# AI-Driven Workflow Orchestrator

This project demonstrates how to use a Large Language Model (Google's Gemini) as a reasoning engine to orchestrate a business workflow. Instead of using hardcoded `if/else` logic, the Java application queries the AI at each step to determine the next logical action.

The workflow implemented is based on a resource requisition process:



## Architecture

*   **Backend:** Java 17, Spring Boot 3
*   **AI Model:** Google Gemini Pro
*   **Database:** H2 In-Memory Database
*   **Frontend:** HTML, CSS, Vanilla JavaScript
*   **Build Tool:** Maven

## How It Works

The core of the system is the `WorkflowOrchestratorService`.

1.  A user submits a resource request via the web UI.
2.  The `WorkflowController` receives the request and calls `WorkflowOrchestratorService`.
3.  The service creates an initial `Requisition` record in the H2 database.
4.  It enters a loop, where at each iteration:
    a. It constructs a detailed **prompt** for the Gemini API. This prompt includes the overall workflow rules, the list of possible actions (in a strict JSON format), and the *current state* of the process (e.g., "Requisition submitted").
    b. The `GeminiService` sends this prompt to the Gemini API.
    c. The AI analyzes the context and returns a JSON object specifying the next `action` and its `reasoning`.
    d. The orchestrator parses the JSON and executes the corresponding Java method (e.g., `checkInternalAvailability()`, `createRFP()`).
    e. The action's result updates the `currentState` variable.
    f. The process and the AI's reasoning are appended to a log.
5.  The loop continues until the AI returns an `END_WORKFLOW` action or an error occurs.
6.  The final, detailed log is sent back to the frontend to be displayed to the user.

This model makes the AI's "thinking" process transparent and central to the application's logic.

## Prerequisites

*   JDK 17 or later
*   Maven 3.6+
*   A Google Gemini API Key

## Setup and Running the Project

1.  **Clone the repository:**
    ```bash
    git clone <your-repo-url>
    cd ai-workflow
    ```

2.  **Set the API Key:**
    The application reads the Gemini API key from an environment variable named `GEMINI_API_KEY`.

    **For Linux/macOS:**
    ```bash
    export GEMINI_API_KEY="your_actual_api_key_here"
    ```

    **For Windows (Command Prompt):**
    ```bash
    set GEMINI_API_KEY="your_actual_api_key_here"
    ```
    Alternatively, you can configure this environment variable directly in your IDE's run configuration for the project.

3.  **Run the application:**
    ```bash
    mvn spring-boot:run
    ```

4.  **Access the application:**
    *   **Frontend UI:** Open your browser and go to `http://localhost:8080`
    *   **H2 Database Console:** Go to `http://localhost:8080/h2-console`
        *   JDBC URL: `jdbc:h2:mem:workflowdb`
        *   User Name: `sa`
        *   Password: `password`
          

## API Endpoint

*   **`POST /api/workflow/start`**
    *   Starts a new workflow instance.
    *   **Request Body:**
        ```json
        {
          "resourceDetails": "Senior Backend Engineer"
        }
        ```
    *   **Success Response (200 OK):**
        ```json
        {
          "log": "Step 1: User submitted requisition...\n\nAI Decision:...\nAI Action:...\nSystem Action:...\n\n..."
        }
        ```
