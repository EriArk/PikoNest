# PIKOOS experimental transport. Appended to the pinned upstream launch script.
# All files are ordinary .p8; this envelope is never the editable project format.
const PIKOOS_INVALID_SET = "!pikoos-invalid-set!"
var _pikoos_set_root = ""
var _pikoos_set_files: PackedStringArray = []

func _pikoos_release_set() -> void:
	# Called only after the monitored runtime process exits. Exact owned paths,
	# no recursive deletion; leave anything unexpected for diagnostics.
	if _pikoos_set_root.is_empty():
		return
	var directory = DirAccess.open(_pikoos_set_root)
	if not directory or not directory.get_directories().is_empty():
		return
	var names = directory.get_files()
	if names.size() != _pikoos_set_files.size():
		return
	for filename in names:
		if not _pikoos_set_files.has(filename):
			return
	if OS.execute("/system/bin/chmod", ["700", _pikoos_set_root], []) != 0:
		return
	for filename in names:
		if DirAccess.remove_absolute(_pikoos_set_root + "/" + filename) != OK:
			return
	DirAccess.remove_absolute(_pikoos_set_root)
	_pikoos_set_root = ""
	_pikoos_set_files.clear()

func _pikoos_set_error() -> String:
	push_error("PIKOOS: invalid or unavailable runtime file set")
	var dialog = AcceptDialog.new()
	dialog.title = "PIKOOS"
	dialog.dialog_text = "Не удалось подготовить части игры. Вернись в PIKOOS и повтори запуск."
	dialog.confirmed.connect(func(): get_tree().quit())
	dialog.canceled.connect(func(): get_tree().quit())
	add_child(dialog)
	dialog.popup_centered(Vector2i(600, 220))
	return PIKOOS_INVALID_SET

func _pikoos_set_name(input: FileAccess) -> String:
	if input.get_position() + 4 > input.get_length():
		return ""
	var length = input.get_32()
	if length < 4 or length > 120 or input.get_position() + length > input.get_length():
		return ""
	var raw = input.get_buffer(length)
	var value = raw.get_string_from_ascii()
	var pattern = RegEx.new()
	pattern.compile("^[A-Za-z0-9_-][A-Za-z0-9_.-]*\\.p8$")
	var match = pattern.search(value)
	if not match or match.get_string() != value or value.contains("..") or value.to_ascii_buffer() != raw:
		return ""
	return value

func _pikoos_unpack_set(path: String) -> String:
	var input = FileAccess.open(path, FileAccess.READ)
	if not input or input.get_length() < 20 or input.get_length() > 8 * 1024 * 1024 + 8192:
		return _pikoos_set_error()
	input.big_endian = true
	if input.get_buffer(8).get_string_from_ascii() != "PIKOSET1":
		return _pikoos_set_error()
	var entry = _pikoos_set_name(input)
	if entry.is_empty() or input.get_position() + 4 > input.get_length():
		return _pikoos_set_error()
	var count = input.get_32()
	if count < 1 or count > 32:
		return _pikoos_set_error()
	var files = {}
	var folded = {}
	var total = 0
	for i in range(count):
		var filename = _pikoos_set_name(input)
		if filename.is_empty() or folded.has(filename.to_lower()) or input.get_position() + 4 > input.get_length():
			return _pikoos_set_error()
		var size = input.get_32()
		total += size
		if size < 1 or size > 2 * 1024 * 1024 or total > 8 * 1024 * 1024 or input.get_position() + size > input.get_length():
			return _pikoos_set_error()
		files[filename] = input.get_buffer(size)
		folded[filename.to_lower()] = true
	if input.get_position() != input.get_length() or not files.has(entry):
		return _pikoos_set_error()
	input.close()
	# Validate the complete envelope before any extraction. Never reuse a game directory.
	var base = PicoBootManager.APPDATA_FOLDER + "/pikoos-sets"
	if DirAccess.make_dir_recursive_absolute(base) != OK:
		return _pikoos_set_error()
	var directory = DirAccess.open(base)
	# Retain interrupted/failed sessions for diagnostics, with a bounded quota.
	if not directory or directory.get_directories().size() >= 32:
		return _pikoos_set_error()
	var root = base + "/" + Crypto.new().generate_random_bytes(16).hex_encode()
	if DirAccess.dir_exists_absolute(root) or DirAccess.make_dir_absolute(root) != OK:
		return _pikoos_set_error()
	for filename in files:
		var target = root + "/" + filename
		var output = FileAccess.open(target, FileAccess.WRITE)
		if not output:
			return _pikoos_set_error()
		output.store_buffer(files[filename])
		output.flush()
		if output.get_error() != OK:
			return _pikoos_set_error()
		output.close()
		if FileAccess.get_file_as_bytes(target) != files[filename]:
			return _pikoos_set_error()
		# Ordinary cartdata/dset remains in the runtime's persistent home. File-set
		# sources themselves are read-only; never silently discard cstore/save writes.
		# Godot's Android FileAccess may not implement chmod. Use Android's
		# fixed executable with argument-array passing, never a shell command.
		if OS.execute("/system/bin/chmod", ["444", target], []) != 0:
			return _pikoos_set_error()
	if OS.execute("/system/bin/chmod", ["555", root], []) != 0:
		return _pikoos_set_error()
	_pikoos_set_root = root
	_pikoos_set_files = PackedStringArray(files.keys())
	print("PIKOOS: prepared file set: ", count, " files, entry ", entry)
	return root + "/" + entry
