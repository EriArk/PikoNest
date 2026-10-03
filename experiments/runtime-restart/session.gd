# Android adapter journal only. No custom API or data enters the cartridge.
var _pikoos_play_token = ""
var _pikoos_play_identity = ""

func _pikoos_session_started(pid: int) -> void:
	var path = PicoBootManager.APPDATA_FOLDER + "/play-session.txt"
	var file = FileAccess.open(path, FileAccess.READ)
	if not file:
		return
	var fields = file.get_as_text().split("\n")
	file.close()
	if fields.size() != 4 or fields[1] != "PREPARING":
		return
	_pikoos_play_token = fields[0]
	var output = []
	OS.execute("/system/bin/cat", ["/proc/" + str(pid) + "/stat"], output)
	var stat = str(output[0]).strip_edges() if not output.is_empty() else ""
	var identity = stat.substr(stat.rfind(")") + 2).split(" ")
	if identity.size() <= 19:
		return # No fabricated exit if identity cannot be established.
	_pikoos_play_identity = str(pid) + "\n" + identity[19]
	_pikoos_session_write("RUNNING")

func _pikoos_session_write(phase: String) -> void:
	if _pikoos_play_token.is_empty() or _pikoos_play_identity.is_empty():
		return
	var path = PicoBootManager.APPDATA_FOLDER + "/play-session.txt"
	var file = FileAccess.open(path + ".new", FileAccess.WRITE)
	if file:
		file.store_string(_pikoos_play_token + "\n" + phase + "\n" + _pikoos_play_identity)
		file.flush()
		file.close()
		DirAccess.rename_absolute(path + ".new", path)
