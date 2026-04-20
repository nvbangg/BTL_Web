(function (window) {
  const BASE_URL_STORAGE_KEY = "fashon.api.baseUrl";
  const DEFAULT_BASE_URL = "http://localhost:8080";

  function normalizeBaseUrl(value) {
    const raw = (value || "").trim();
    const base = raw || DEFAULT_BASE_URL;
    return base.replace(/\/+$/, "");
  }

  function getBaseUrl() {
    return normalizeBaseUrl(localStorage.getItem(BASE_URL_STORAGE_KEY));
  }

  function setBaseUrl(value) {
    const normalized = normalizeBaseUrl(value);
    localStorage.setItem(BASE_URL_STORAGE_KEY, normalized);
    return normalized;
  }

  function buildApiUrl(path) {
    const normalizedPath = path.startsWith("/") ? path : "/" + path;
    return getBaseUrl() + normalizedPath;
  }

  function buildImageUrl(fileName) {
    if (!fileName) return "";
    const normalizedFile = String(fileName).trim();
    if (!normalizedFile) return "";
    return getBaseUrl() + "/assets/images/" + encodeURIComponent(normalizedFile).replace(/%2F/g, "/");
  }

  window.AppConfig = {
    BASE_URL_STORAGE_KEY,
    DEFAULT_BASE_URL,
    getBaseUrl,
    setBaseUrl,
    buildApiUrl,
    buildImageUrl
  };
})(window);
