const token = localStorage.getItem("authToken");
if (!token) window.location.href = "/login.html";

document.getElementById("logoutBtn").addEventListener("click", () => {
    localStorage.removeItem("authToken");
    window.location.href = "/login.html";
});

document.getElementById("fileInput").addEventListener("change", handleFile);

function formatExcelDate(value) {
    if (typeof value === "number") {
        const d = XLSX.SSF.parse_date_code(value);
        if (d) {
            return `${String(d.d).padStart(2, "0")}/${String(d.m).padStart(2, "0")}/${d.y} ${String(d.H).padStart(2, "0")}:${String(d.M).padStart(2, "0")}:${String(Math.floor(d.S)).padStart(2, "0")}`;
        }
    }
    if (typeof value === "string") {
        const d = new Date(value);
        if (!isNaN(d)) {
            return `${String(d.getDate()).padStart(2, "0")}/${String(d.getMonth() + 1).padStart(2, "0")}/${d.getFullYear()} ${String(d.getHours()).padStart(2, "0")}:${String(d.getMinutes()).padStart(2, "0")}:${String(d.getSeconds()).padStart(2, "0")}`;
        }
    }
    return value ?? "";
}

let currentHeader = [];
let currentRows = [];

function handleFile(e) {
    const file = e.target.files[0];
    if (!file) return;
    const reader = new FileReader();
    reader.onload = function (evt) {
        const data = new Uint8Array(evt.target.result);
        const workbook = XLSX.read(data, { type: "array" });
        const sheet = workbook.Sheets[workbook.SheetNames[0]];
        const rows = XLSX.utils.sheet_to_json(sheet, { header: 1 });

        currentHeader = rows[0] || [];
        currentRows = rows.slice(1) || [];
        renderTable(currentHeader, currentRows);
    };
    reader.readAsArrayBuffer(file);
}

function updateCounters() {
    const rows = document.querySelectorAll("#preview table tbody tr");
    let shown = 0;
    let selected = 0;

    rows.forEach(tr => {
        if (tr.style.display !== "none") {
            shown++;
            const checkbox = tr.querySelector("input[type=checkbox]");
            if (checkbox && checkbox.checked) {
                selected++;
            }
        }
    });

    document.getElementById("countShown").textContent = "Registros mostrados: " + shown;
    document.getElementById("countSelected").textContent = "Registros seleccionados: " + selected;
}

function renderTable(header, dataRows) {
    const preview = document.getElementById("preview");
    const fechaFiltro = document.getElementById("fechaFiltro");

    preview.innerHTML = "";

    const idxFechaPagoExacta = header.findIndex(h =>
        h?.toString().toLowerCase().includes("fecha de pago")
    );
    const idxPrimeraFecha = header.findIndex(h =>
        h?.toString().toLowerCase().includes("fecha")
    );
    const FECHA_PAGO_INDEX = idxFechaPagoExacta >= 0 ? idxFechaPagoExacta : idxPrimeraFecha;

    const FECHA_INDEXES = header
        .map((h, i) => h?.toString().toLowerCase().includes("fecha") ? i : -1)
        .filter(i => i >= 0);

    if (dataRows.length === 0) {
        preview.textContent = "El archivo está vacío.";
        fechaFiltro.innerHTML = `<option value="__all__">Todas</option>`;
        updateCounters();
        return;
    }

    const normalized = dataRows.map(row => {
        const fixed = [...row];
        while (fixed.length < header.length) fixed.push("");
        FECHA_INDEXES.forEach(idx => {
            fixed[idx] = formatExcelDate(fixed[idx]);
        });
        return fixed;
    });

    const fechasPagoSet = new Set();
    if (FECHA_PAGO_INDEX >= 0) {
        normalized.forEach(r => {
            const val = r[FECHA_PAGO_INDEX] || "";
            if (val && val !== "__sin_fecha__") fechasPagoSet.add(val);
        });
    }
    fechaFiltro.innerHTML = `<option value="__all__">Todas</option>`;
    Array.from(fechasPagoSet).sort().forEach(f => {
        const opt = document.createElement("option");
        opt.value = f;
        opt.textContent = f;
        fechaFiltro.appendChild(opt);
    });

    let html = "<table><thead><tr><th>Cargar</th><th>Observación</th>";
    header.forEach(col => html += `<th>${col || "Columna"}</th>`);
    html += "</tr></thead><tbody>";

    let groupIndex = 0;
    const groups = {};
    normalized.forEach(r => {
        const key = FECHA_PAGO_INDEX >= 0 ? (r[FECHA_PAGO_INDEX] || "__sin_fecha__") : "__todas__";
        if (!groups[key]) groups[key] = [];
        groups[key].push(r);
    });

    Object.keys(groups).forEach(key => {
        const colorClass = "group-" + (groupIndex % 5);
        groups[key].forEach(row => {
            html += `<tr class="${colorClass}" data-fecha-pago="${key}">`;
            html += `<td><input type="checkbox" checked /></td>`;
            html += `<td><input type="text" placeholder="Observación..." /></td>`;
            row.forEach(cell => {
                let val = cell ?? "";
                if (typeof val === "number") {
                    // fuerza a string para evitar notación científica
                    val = val.toLocaleString("en-US", { useGrouping: false });
                }
                html += `<td>${val}</td>`;
            });

            html += "</tr>";
        });
        groupIndex++;
    });

    html += "</tbody></table>";
    preview.innerHTML = html;

    fechaFiltro.onchange = () => {
        const valor = fechaFiltro.value;
        document.querySelectorAll("#preview table tbody tr").forEach(tr => {
            const fp = tr.dataset.fechaPago;
            tr.style.display = (valor === "__all__" || fp === valor) ? "" : "none";
        });
        updateCounters();
    };

    document.querySelectorAll("#preview table tbody tr input[type=checkbox]").forEach(chk => {
        chk.addEventListener("change", updateCounters);
    });

    updateCounters();
}
/**
 * Subir seleccionados → /uploadSelected
 */
document.getElementById("uploadForm").addEventListener("submit", async (e) => {
    e.preventDefault();
    const statusDiv = document.getElementById("status");
    const rows = [];

    document.querySelectorAll("#preview table tbody tr").forEach(tr => {
        if (tr.style.display === "none") return;
        const checkbox = tr.querySelector("input[type=checkbox]");
        const textInput = tr.querySelector("input[type=text]");
        if (checkbox && checkbox.checked) {
            const cells = Array.from(tr.querySelectorAll("td")).slice(2).map(td => td.textContent);
            rows.push({ extraTexto: textInput.value, datos: cells });
        }
    });

    if (rows.length === 0) {
        statusDiv.textContent = "No seleccionaste registros.";
        statusDiv.className = "error";
        return;
    }

    try {
        const res = await fetch("/api/secure/uploadSelected", {
            method: "POST",
            headers: {
                "Authorization": "Bearer " + token,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(rows)
        });
        const data = await res.json().catch(() => ({}));
        statusDiv.textContent = res.ok ? (data.mensaje || "Registros enviados.") : (data.error || "Error al enviar.");
        statusDiv.className = res.ok ? "success" : "error";
    } catch (err) {
        statusDiv.textContent = "Error de red o servidor";
        statusDiv.className = "error";
    }
});

/**
 * Cargar archivo completo → /upload
 */
document.getElementById("uploadAllBtn").addEventListener("click", async () => {
    const statusDiv = document.getElementById("status");
    const fileInput = document.getElementById("fileInput");
    const file = fileInput.files[0];

    if (!file) {
        statusDiv.textContent = "No seleccionaste ningún archivo.";
        statusDiv.className = "error";
        return;
    }

    const formData = new FormData();
    formData.append("file", file);

    try {
        const res = await fetch("/api/secure/upload", {
            method: "POST",
            headers: {
                "Authorization": "Bearer " + token
            },
            body: formData
        });
        const data = await res.json().catch(() => ({}));
        statusDiv.textContent = res.ok ? (data.mensaje || "Archivo completo enviado.") : (data.error || "Error al enviar.");
        statusDiv.className = res.ok ? "success" : "error";

    } catch (err) {
        statusDiv.textContent = "Error de red o servidor";
        statusDiv.className = "error";
    }
});
