# Executable candidates arrive only through the signature-protected Java gate.
var _pikoos_active_probe = ""

func _pikoos_probe_status(phase: String, pid: int = 0) -> void:
	var state = phase
	if phase == "RUNNING":
		# Android's Godot FileAccess cannot reliably read procfs. Use fixed argv.
		var output = []
		OS.execute("/system/bin/cat", ["/proc/" + str(pid) + "/stat"], output)
		var stat = str(output[0]).strip_edges() if not output.is_empty() else ""
		var fields = stat.substr(stat.rfind(")") + 2).split(" ")
		if fields.size() > 19:
			state += "\n" + str(pid) + "\n" + fields[19]
		# Missing identity stays conservatively RUNNING, never a false exit.
	var file = FileAccess.open(PicoBootManager.APPDATA_FOLDER + "/probe-status.txt", FileAccess.WRITE)
	if file:
		file.store_string(state)
		file.flush()

func _pikoos_probe_path(path: String) -> bool:
	var prefix = PicoBootManager.APPDATA_FOLDER + "/pikoos-probes/"
	if not path.begins_with(prefix):
		return false
	var relative = path.substr(prefix.length())
	var pattern = RegEx.new()
	pattern.compile("^[0-9a-f]{32}/probe\\.p8$")
	var found = pattern.search(relative)
	return found != null and found.get_string() == relative

func _pikoos_finish_probe() -> void:
	if _pikoos_active_probe.is_empty():
		return
	_pikoos_probe_status("EXITED")
	# Preserve isolated home/logs for diagnosis. Binary payloads are disposable.
	for filename in ["pico8_64", "pico8.dat", "probe.p8"]:
		DirAccess.remove_absolute(_pikoos_active_probe + "/" + filename)
	var closed = FileAccess.open(_pikoos_active_probe + "/closed", FileAccess.WRITE)
	if closed:
		closed.store_string("EXITED")
	_pikoos_active_probe = ""
