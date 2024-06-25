// This file was auto generated by build.gradle.kts
package meetnote3.static

const val FRONTEND_HTML = """
<html>
<head>
  <title>Meetnote3</title>
  <link rel="stylesheet" href="/frontend.css" type="text/css" />
  <script src="/frontend.js"></script>
</head>
<body>
<h1>Hello, Meetnote3!</h1>
<div class="row">
  <div id="sidebar">
    <h2>Current Child Procs</h2>
    <ul id="child-procs-container">
      <!-- lazy load from /child-procs -->
    </ul>
    <h2>System Logs</h2>
    <ul id="system-logs-container">
      <!-- lazy load -->
    </ul>
    <h2>Meeting Logs</h2>
    <ul id="meeting-logs-container">
      <!-- lazy load -->
    </ul>
  </div>
  <div class="column" id="main-content">
    render the content when click the sidebar menu.
  </div>
</div>
</body>
</html>

"""