# Agent Lessons

## Git Workflows

### Commits and Pushing
* **Pattern**: Committing and pushing directly to shared branches like `main` or `develop`.
* **Rule**: Always create a feature branch (e.g., `feature/description` or `fix/description`) when working on new tasks or fixes. Do not push directly to `main` or `develop` unless explicitly instructed by the user. Submit code via branch pushes so it can be merged via pull/merge requests.

## Network and Environment

### VPN & External API Connections
* **Pattern**: Retrying or attempting to debug connection timeout/failure errors when calling external APIs (e.g. m-smart, CBS).
* **Rule**: This project requires a VPN connection to access external APIs. If a connection error occurs, do not attempt to retry or troubleshoot. Immediately stop and inform the user that the VPN may be disconnected.

