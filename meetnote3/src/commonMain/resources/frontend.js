document.addEventListener("DOMContentLoaded", async function () {
  startChildProcessMonitor();
  await loadSystemLogs()
  await loadMeetingLogs()
})

function startChildProcessMonitor() {
  setInterval(async () => {
    const procs = await (await fetch("/api/child-procs")).json()
    const procsContainer = document.getElementById("child-procs-container")
    procsContainer.innerHTML = ""
    for (let proc of procs) {
      const li = document.createElement("li")
      li.innerText = proc["pid"] + " " + proc["name"]
      procsContainer.appendChild(li)
    }
  }, 1000);
}

async function loadSystemLogs() {
  const systemLogs = await (await fetch("/api/system-logs")).json()
  const systemLogsContainer = document.getElementById("system-logs-container")
  for (let systemLog of systemLogs) {
    const li = document.createElement("li")
    const a = document.createElement("a")
    a.innerText = systemLog
    a.addEventListener("click", async () => {
      location.hash = "#/system-logs/" + systemLog
      const status = await (await fetch("/api/system-logs/" + systemLog)).text()
      const mainContent = document.getElementById("main-content")
      mainContent.innerHTML = ""
      const pre = document.createElement("pre")
      pre.innerText = status
      mainContent.appendChild(pre)
      return false;
    });
    li.appendChild(a)
    systemLogsContainer.appendChild(li)
  }
}

async function loadMeetingLogs() {
  const meetingLogs = await (await fetch("/api/meeting-logs")).json()
  const meetingLogsContainer = document.getElementById("meeting-logs-container")
  for (let meetingLog of meetingLogs) {
    const li = document.createElement("li")
    const a = document.createElement("a")
    a.innerText = meetingLog.shortName + " " + meetingLog.duration
    a.addEventListener("click", async () => {
      location.hash = "#/meeting-logs/" + meetingLog.name
      const data = await (await fetch("/api/meeting-logs/" + meetingLog.name)).json()
      const mainContent = document.getElementById("main-content")
      mainContent.innerHTML = ""

      {
        const h1 = document.createElement("h1")
        h1.innerText = meetingLog.name
        mainContent.appendChild(h1)
      }
      {
        const h2 = document.createElement("h2")
        h2.innerText = "Summary"
        mainContent.appendChild(h2)
      }
      {
        const pre = document.createElement("pre")
        pre.innerText = data.summary
        mainContent.appendChild(pre)
      }
      {
        const h2 = document.createElement("h2")
        h2.innerText = "Transcript"
        mainContent.appendChild(h2)
      }
      {
        console.log(data)
        const pre = document.createElement("pre")
        pre.innerText = data.lrc
        mainContent.appendChild(pre)
      }
      return false;
    });
    li.appendChild(a)
    meetingLogsContainer.appendChild(li)
  }
}
