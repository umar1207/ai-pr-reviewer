# AI-Driven Code Review Tool for PR Merging
Automatically reviews your pull requests using AI and helps maintain high code quality before merging.

---

## 🛠️ Usage

1. Go to your repository: **Settings → Webhooks → Add Webhook**
2. Set the **Payload URL** to:  
   [https://immortal-spaniel-unique.ngrok-free.app/api/v1/review/pr](https://immortal-spaniel-unique.ngrok-free.app/api/v1/review/pr)
3. Set **Content type** to: `application/json`
4. Choose **Let me select individual events**, and select only: `pull_request`
5. Click **Add Webhook**

---

> ⚠️ Make sure your server is running at the above URL before triggering pull request events.
