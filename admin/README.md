TestSync admin dashboard

Files:
- dashboard.html - main admin UI. Open in browser at http://localhost:8080/dashboard.html after starting AdminHTTPServer.
- styles.css - styles used by the UI
- dashboard.js - client-side JS that polls real-time API endpoints (/api/status, /api/students, /api/results) every 5s

How to run (4 separate terminals):

1. Start the TimerBroadcaster (countdown):
   ```
   java -cp . broadcaster.TimerBroadcaster
   ```

2. Start the Admin HTTP Server (serves dashboard + API):
   ```
   java -cp . server.AdminHTTPServer 8080
   ```
   Then open http://localhost:8080/dashboard.html in your browser

3. Start the Exam Server (accepts student connections):
   ```
   java -cp . server.ExamServer
   ```
   Type START to begin the exam once students connect

4. Start StudentClient(s) (one or more students):
   ```
   java -cp . client.StudentClient
   ```
   Login: student1 / pass123 (or student2, student3, etc.)
   After login, students will see "Quiz Yet to Start"

Then type START in the ExamServer terminal to begin the exam.

Real-time updates:
- Dashboard polls /api/status, /api/students, /api/results every 5 seconds
- Exam Status card shows WAITING/RUNNING state
- Students card shows connected students and total registered
- Activity Log shows exam results as students submit answers
- Shared state file (exam_state.txt) persists data between ExamServer and AdminHTTPServer processes

Notes:
- All 4 processes must be running for the full experience
- The shared state file (exam_state.txt) is created automatically in the project root
- Start/Stop buttons on dashboard are UI-only (use ExamServer console to type START)

