# Run a session on the classroom Wi-Fi

1. Connect the teacher’s computer and the students’ phones to the same Wi-Fi. The network must allow devices to communicate with each other.
2. Build the classroom jar with `scripts/build-local-live.sh`. Run `./scripts/local-live/start.sh` from the repository root to start the app with the existing development database.
3. Open `http://localhost:8080` on the teacher’s computer, choose a published assessment and start a live session.
4. Students scan the QR on the session screen to open the class in their phone browser.

If several network interfaces are available, set `LOCAL_LIVE_HOST` to the computer’s Wi-Fi IPv4 address before starting the app.
