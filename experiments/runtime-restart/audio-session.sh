# Sourced from start_pico_proot.sh, in the wrapper's private package directory.
# The upstream background cleanup could unlink the directory already opened by
# FD 7. Complete cleanup here before starting the server, then wait for its socket.
./busybox killall pulseaudio 2>/dev/null || true
./busybox rm -rf tmp/pulse
LD_LIBRARY_PATH=. ./busybox ash ./pulsar.sh > "$LOG_DIR/pulse.log" 2>&1 &
PIKOOS_AUDIO_PID=$!

pikoos_audio_cleanup() {
    kill "$PIKOOS_AUDIO_PID" 2>/dev/null || true
    wait "$PIKOOS_AUDIO_PID" 2>/dev/null || true
}
trap pikoos_audio_cleanup EXIT
trap 'exit 130' INT
trap 'exit 143' TERM HUP

PIKOOS_AUDIO_TRIES=0
while [ ! -S tmp/pulse/pulse ]; do
    if ! kill -0 "$PIKOOS_AUDIO_PID" 2>/dev/null; then
        echo "PIKOOS: audio server exited before socket readiness" >&2
        exit 1
    fi
    PIKOOS_AUDIO_TRIES=$((PIKOOS_AUDIO_TRIES + 1))
    if [ "$PIKOOS_AUDIO_TRIES" -ge 250 ]; then
        echo "PIKOOS: audio socket readiness timed out" >&2
        exit 1
    fi
    sleep 0.02
done
echo "PIKOOS: audio socket ready (pid $PIKOOS_AUDIO_PID)"
