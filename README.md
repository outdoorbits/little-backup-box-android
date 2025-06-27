# LBB Scanner Android app
Android app to find a Little Backup Box on the local network

## How the App Works

This app is designed to detect an active **Little Backup Box** on the same local network as the device running the app.

To do this, the app performs the following steps:

1. It determines the local subnet (e.g., `192.168.0.*`).
2. It iterates over the last part of the IP address range (`192.168.0.1` to `192.168.0.254`) and checks each address for a response from a device that matches the expected signature of a Little Backup Box.
3. For any matching device found, the app displays relevant links (e.g., to the web interface or software resources).

---

### Important Notes

- All scanning is performed **entirely within the local network**.
- No data is sent or received from the internet.
- No user data is collected, stored, or transmitted.
- The app requires local network access permission solely for this scanning process.

Please note: This only works if the LBB software is on or after June 1, 2025.

Check for m ore information about [Little Backup Box](https://github.com/outdoorbits/little-backup-box) on GitHub.
