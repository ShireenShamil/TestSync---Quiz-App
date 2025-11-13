// Dashboard JS: polls API endpoints for live exam data and updates UI
(function(){
  const logEl = document.getElementById('log');
  const refreshBtn = document.getElementById('refreshBtn');
  const resultsBtn = document.getElementById('resultsBtn');
  const startBtn = document.getElementById('startBtn');
  const stopBtn = document.getElementById('stopBtn');

  function appendLog(line){
    const t = new Date().toLocaleTimeString();
    logEl.textContent = `[${t}] ${line}\n` + logEl.textContent;
  }

  async function fetchAndUpdate(){
    try{
      // Fetch exam status (state, connected students, etc.)
      const statusRes = await fetch('/api/status');
      if(statusRes.ok){
        const statusData = await statusRes.json();
        document.getElementById('state').textContent = statusData.state || 'WAITING';
        document.getElementById('totalRegistered').textContent = statusData.totalRegistered || 0;
        document.getElementById('lastUpdated').textContent = new Date().toLocaleString();
      }

      // Fetch student counts
      const studentRes = await fetch('/api/students');
      if(studentRes.ok){
        const studentData = await studentRes.json();
        document.getElementById('connectedWaiting').textContent = studentData.connected || 0;
      }

      // Fetch results (JSON format)
      const resultsRes = await fetch('/api/results');
      if(resultsRes.ok){
        const resultsData = await resultsRes.json();
        let logText = `----- Exam Results (${new Date().toLocaleTimeString()}) -----\n`;
        if(!resultsData.submissions || resultsData.submissions.length === 0){
          logText += 'No results submitted yet.';
        }else{
          logText += `Total Submissions: ${resultsData.totalSubmissions}\n\n`;
          for(const sub of resultsData.submissions){
            logText += `>>> ${sub.student}: ${sub.scorePercent}%\n`;
            for(const ans of sub.answers){
              const mark = ans.correct ? '[OK]' : '[XX]';
              logText += `    ${mark} Q: ${ans.question}\n`;
              logText += `         Answer: ${ans.answer}\n`;
            }
            logText += '\n';
          }
        }
        appendLog(logText);
      }
    }catch(err){
      appendLog('API Error: ' + err.message);
    }
  }

  refreshBtn.addEventListener('click', fetchAndUpdate);
  resultsBtn.addEventListener('click', fetchAndUpdate);

  startBtn.addEventListener('click', async ()=>{
    try {
      const res = await fetch('/api/start', { method: 'POST' });
      if(res.ok){
        document.getElementById('state').textContent = 'Running';
        appendLog('✓ Exam started successfully!');
        document.getElementById('lastUpdated').textContent = new Date().toLocaleString();
      } else {
        appendLog('✗ Failed to start exam');
      }
    } catch(err) {
      appendLog('✗ Error starting exam: ' + err.message);
    }
  });
  stopBtn.addEventListener('click', async ()=>{
    try {
      const res = await fetch('/api/stop', { method: 'POST' });
      if(res.ok){
        document.getElementById('state').textContent = "Stopped";
        appendLog('✓ Exam stopped successfully!');
        document.getElementById('lastUpdated').textContent = new Date().toLocaleString();
      } else {
        appendLog('✗ Failed to stop exam');
      }
    } catch(err) {
      appendLog('✗ Error stopping exam: ' + err.message);
    }
  });

  // initial poll
  appendLog('Dashboard loaded');
  fetchAndUpdate();

  // periodic poll every 5s
  setInterval(fetchAndUpdate, 5000);
})();
