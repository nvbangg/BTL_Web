import { createStatusLabel } from "./utils.js";

function ensureToastRoot() {
  let root = document.getElementById("toast-root");
  if (!root) {
    root = document.createElement("div");
    root.id = "toast-root";
    root.className = "toast-root";
    document.body.appendChild(root);
  }
  return root;
}

export function showToast(message, type = "info", timeout = 2600) {
  const root = ensureToastRoot();
  const item = document.createElement("div");
  item.className = `toast toast-${type}`;
  item.textContent = message;
  root.appendChild(item);
  requestAnimationFrame(() => item.classList.add("toast-show"));
  setTimeout(() => {
    item.classList.remove("toast-show");
    setTimeout(() => item.remove(), 200);
  }, timeout);
}

export function clearFieldErrors(form) {
  if (!form) {
    return;
  }
  form.querySelectorAll(".field-error").forEach((node) => node.remove());
}

export function renderFieldErrors(form, errors = []) {
  clearFieldErrors(form);
  if (!Array.isArray(errors)) {
    return;
  }
  errors.forEach((error) => {
    if (!error?.field) {
      return;
    }
    const field = form.querySelector(`[name=\"${error.field}\"]`);
    if (!field) {
      return;
    }
    const hint = document.createElement("div");
    hint.className = "field-error";
    hint.textContent = error.message || "Du lieu khong hop le";
    field.insertAdjacentElement("afterend", hint);
  });
}

export function openModal(contentHtml, options = {}) {
  const overlay = document.createElement("div");
  overlay.className = "modal-overlay";

  const modal = document.createElement("div");
  modal.className = `modal-card ${options.className || ""}`.trim();
  modal.innerHTML = contentHtml;
  overlay.appendChild(modal);
  document.body.appendChild(overlay);
  document.body.classList.add("body-lock");

  function close() {
    overlay.remove();
    if (!document.querySelector(".modal-overlay")) {
      document.body.classList.remove("body-lock");
    }
  }

  overlay.addEventListener("click", (event) => {
    if (event.target === overlay && options.closeOnBackdrop !== false) {
      close();
    }
  });

  modal.querySelectorAll("[data-close-modal]").forEach((button) => {
    button.addEventListener("click", close);
  });

  return { overlay, modal, close };
}

export function setButtonLoading(button, loading, textBackup = "") {
  if (!button) {
    return;
  }
  if (loading) {
    button.dataset.backupText = button.textContent;
    button.disabled = true;
    button.textContent = textBackup || "Dang xu ly...";
  } else {
    button.disabled = false;
    button.textContent = button.dataset.backupText || button.textContent;
  }
}

export function renderPagination(container, options) {
  if (!container) {
    return;
  }
  const total = Number(options.total || 0);
  const pageSize = Number(options.pageSize || 10);
  const currentPage = Math.max(1, Number(options.page || 1));
  const totalPages = Math.max(1, Math.ceil(total / pageSize));

  if (totalPages <= 1) {
    container.innerHTML = "";
    return;
  }

  const pages = [];
  const start = Math.max(1, currentPage - 2);
  const end = Math.min(totalPages, start + 4);
  for (let p = start; p <= end; p += 1) {
    pages.push(p);
  }

  container.innerHTML = `
    <button class="page-btn" data-page="${Math.max(1, currentPage - 1)}" ${currentPage === 1 ? "disabled" : ""}>Prev</button>
    ${pages
      .map(
        (page) =>
          `<button class="page-btn ${page === currentPage ? "active" : ""}" data-page="${page}">${page}</button>`
      )
      .join("")}
    <button class="page-btn" data-page="${Math.min(totalPages, currentPage + 1)}" ${currentPage === totalPages ? "disabled" : ""}>Next</button>
  `;

  container.querySelectorAll(".page-btn").forEach((button) => {
    button.addEventListener("click", () => {
      const page = Number(button.dataset.page);
      if (!page || page === currentPage) {
        return;
      }
      options.onChange?.(page);
    });
  });
}

export function statusBadge(status) {
  return `<span class="status-badge status-${status || "pending"}">${createStatusLabel(status)}</span>`;
}
