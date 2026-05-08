<template>
  <div class="page">
    <header class="top-bar">
      <div class="top-bar-inner">
        <router-link to="/" class="brand">
          <span class="logo">&#9650;</span>
          <span>IoTeca</span>
        </router-link>
        <nav class="top-nav">
          <router-link to="/login" class="btn-ghost">Log in</router-link>
          <router-link to="/register" class="btn-primary">Get started</router-link>
        </nav>
      </div>
    </header>

    <div class="content">
      <div class="inner">
        <p class="doc-label">Legal</p>
        <h1>Terms &amp; Conditions</h1>
        <p class="effective">Effective date: 1 May 2026</p>

        <p class="lead">
          By accessing or using IoTeca ("the Service"), you agree to be bound by these Terms and Conditions.
          Please read them carefully before creating an account.
        </p>

        <section>
          <h2>1. Use of the Service</h2>
          <p>IoTeca is a platform for developers and teams to store and process IoT device data. You may use the Service only for lawful purposes and in accordance with these Terms.</p>
          <p>You agree not to:</p>
          <ul>
            <li>Use the Service to store or transmit unlawful, harmful, or malicious data.</li>
            <li>Attempt to gain unauthorised access to any part of the Service or another user's account.</li>
            <li>Circumvent or abuse rate limits or quota systems.</li>
            <li>Use the Service in a way that disrupts its integrity or performance.</li>
          </ul>
        </section>

        <section>
          <h2>2. Account registration</h2>
          <p>
            You must provide accurate and complete information when registering. You are responsible for maintaining
            the confidentiality of your credentials and for all activity that occurs under your account.
            Notify us immediately if you suspect unauthorised access.
          </p>
        </section>

        <section>
          <h2>3. API keys and device access</h2>
          <p>
            API keys issued through the platform allow devices to send data to your projects. You are responsible
            for keeping API keys confidential. Keys should be stored securely on your devices and rotated
            periodically. IoTeca is not liable for data submitted using a compromised key.
          </p>
        </section>

        <section>
          <h2>4. Data and content</h2>
          <p>
            You retain ownership of all data you submit to the Service. You grant IoTeca a limited licence to
            store, process, and transmit your data solely for the purpose of providing the Service.
            IoTeca does not use your device data for advertising or any purpose beyond service delivery.
          </p>
        </section>

        <section>
          <h2>5. Quotas and fair use</h2>
          <p>
            Each account is subject to a daily ingestion quota. Exceeding your quota will result in ingestion
            requests being rejected until the quota resets. IoTeca reserves the right to adjust quota limits
            with reasonable notice. Accounts that consistently abuse the platform may be suspended.
          </p>
        </section>

        <section>
          <h2>6. Service availability</h2>
          <p>
            IoTeca aims to maintain high availability but does not guarantee uninterrupted access to the Service.
            Planned maintenance will be communicated in advance where possible. IoTeca is not liable for losses
            arising from service interruptions.
          </p>
        </section>

        <section>
          <h2>7. Intellectual property</h2>
          <p>
            All software, design, and documentation that make up the IoTeca platform are the property of IoTeca
            and are protected by applicable intellectual property laws. You may not copy, modify, or distribute
            any part of the platform without express written permission.
          </p>
        </section>

        <section>
          <h2>8. Termination</h2>
          <p>
            You may close your account at any time from the account settings page. IoTeca may suspend or terminate
            accounts that violate these Terms, with or without notice depending on severity. Upon termination,
            your data will be deleted in accordance with our
            <router-link to="/privacy">Privacy &amp; Deletion Policy</router-link>.
          </p>
        </section>

        <section>
          <h2>9. Limitation of liability</h2>
          <p>
            To the fullest extent permitted by law, IoTeca is not liable for any indirect, incidental, or
            consequential damages arising from your use of or inability to use the Service, including loss of
            data or revenue.
          </p>
        </section>

        <section>
          <h2>10. Changes to these Terms</h2>
          <p>
            We may update these Terms from time to time. We will notify registered users by email before
            material changes take effect. Continued use of the Service after changes are published constitutes
            acceptance of the updated Terms.
          </p>
        </section>

        <section>
          <h2>11. Governing law</h2>
          <p>
            These Terms are governed by and construed in accordance with applicable law. Any disputes shall be
            resolved in the courts of competent jurisdiction.
          </p>
        </section>

        <section>
          <h2>12. Contact</h2>
          <p>
            For questions about these Terms, contact us at
            <a href="mailto:support@ioteca.io">support@ioteca.io</a>.
          </p>
        </section>

        <div class="nav-row">
          <router-link to="/privacy" class="link-card">Privacy &amp; Deletion Policy &#8594;</router-link>
          <router-link to="/" class="cta-ghost">Back to home</router-link>
        </div>
      </div>
    </div>

    <footer class="footer">
      <div class="footer-inner">
        <nav class="footer-links">
          <router-link to="/">Home</router-link>
          <router-link to="/about">About</router-link>
          <router-link to="/privacy">Privacy &amp; Deletion</router-link>
        </nav>
        <p class="footer-copy">&copy; 2026 IoTeca. All rights reserved.</p>
      </div>
    </footer>
  </div>
</template>

<script>
export default {
  name: 'TermsView',
}
</script>

<style scoped>
.page {
  min-height: 100vh;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
  color: #e0e0e0;
}

/* ── Top bar ── */
.top-bar {
  position: sticky;
  top: 0;
  z-index: 100;
  background: rgba(26, 26, 46, 0.85);
  backdrop-filter: blur(8px);
  border-bottom: 1px solid rgba(255,255,255,0.06);
}
.top-bar-inner {
  max-width: 1100px;
  margin: 0 auto;
  padding: 0 24px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #4fc3f7;
  font-weight: 700;
  font-size: 1.15rem;
  text-decoration: none;
}
.brand .logo { font-size: 1.3rem; }
.top-nav {
  display: flex;
  align-items: center;
  gap: 16px;
}
.top-nav a { text-decoration: none; font-size: 0.9rem; }
.btn-ghost {
  padding: 7px 18px;
  border: 1.5px solid rgba(255,255,255,0.25);
  border-radius: 7px;
  color: #e0e0e0;
  transition: border-color 0.2s;
}
.btn-ghost:hover { border-color: #4fc3f7; color: #4fc3f7; }
.btn-primary {
  padding: 7px 18px;
  background: #1a73e8;
  border-radius: 7px;
  color: #fff;
  font-weight: 500;
}
.btn-primary:hover { background: #1558b0; }

/* ── Content ── */
.content { padding: 64px 24px 80px; }
.inner { max-width: 740px; margin: 0 auto; }

.doc-label {
  font-size: 0.8rem;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: #4fc3f7;
  margin-bottom: 8px;
}
h1 {
  font-size: clamp(1.75rem, 4vw, 2.4rem);
  color: #fff;
  margin-bottom: 8px;
}
.effective { font-size: 0.85rem; color: #546e7a; margin-bottom: 28px; }
.lead {
  font-size: 1rem;
  color: #b0bec5;
  line-height: 1.75;
  margin-bottom: 48px;
  padding: 16px 20px;
  background: rgba(255,255,255,0.04);
  border-left: 3px solid #1a73e8;
  border-radius: 0 8px 8px 0;
}

section { margin-bottom: 40px; }
h2 {
  font-size: 1.05rem;
  color: #fff;
  margin-bottom: 12px;
  padding-bottom: 6px;
  border-bottom: 1px solid rgba(255,255,255,0.08);
}
p {
  color: #90a4ae;
  font-size: 0.9rem;
  line-height: 1.75;
  margin-bottom: 10px;
}
ul {
  padding-left: 20px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
ul li {
  color: #90a4ae;
  font-size: 0.9rem;
  line-height: 1.65;
}
a { color: #4fc3f7; text-decoration: none; }
a:hover { text-decoration: underline; }

/* ── Nav row ── */
.nav-row {
  display: flex;
  gap: 16px;
  flex-wrap: wrap;
  align-items: center;
  margin-top: 48px;
  padding-top: 32px;
  border-top: 1px solid rgba(255,255,255,0.08);
}
.link-card {
  padding: 13px 24px;
  background: rgba(255,255,255,0.05);
  border: 1px solid rgba(255,255,255,0.1);
  border-radius: 8px;
  color: #e0e0e0;
  font-size: 0.9rem;
  text-decoration: none;
  transition: border-color 0.2s;
}
.link-card:hover { border-color: #4fc3f7; color: #4fc3f7; text-decoration: none; }
.cta-ghost {
  padding: 13px 24px;
  border: 1.5px solid rgba(255,255,255,0.2);
  color: #90a4ae;
  border-radius: 8px;
  font-size: 0.9rem;
  text-decoration: none;
  transition: border-color 0.2s;
}
.cta-ghost:hover { border-color: #4fc3f7; color: #4fc3f7; text-decoration: none; }

/* ── Footer ── */
.footer {
  border-top: 1px solid rgba(255,255,255,0.07);
  padding: 32px 24px;
}
.footer-inner {
  max-width: 1100px;
  margin: 0 auto;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}
.footer-links {
  display: flex;
  gap: 24px;
  flex-wrap: wrap;
  justify-content: center;
}
.footer-links a {
  color: #90a4ae;
  text-decoration: none;
  font-size: 0.875rem;
}
.footer-links a:hover { color: #fff; text-decoration: none; }
.footer-copy { font-size: 0.8rem; color: #546e7a; }
</style>
