Build instructions for Phantom AntiCheat

Windows (PowerShell):

  # run with tests
  .\build.ps1

  # skip tests
  .\build.ps1 -SkipTests

Unix / Git Bash / WSL:

  # make executable once
  chmod +x build.sh
  ./build.sh --skip-tests

Notes:
- The scripts call `mvn clean package -U` in the plugin folder.
- Built jar will be placed in `target/phantom-anticheat-<version>.jar`.
