"""
Filestack OCR - Aplicação de Leitura de Imagens
Dependências opcionais: pip install Pillow
"""

import tkinter as tk
from tkinter import ttk, filedialog, messagebox
import json
import threading
import os
import urllib.request
import urllib.error

try:
    from PIL import Image, ImageTk
    PIL_AVAILABLE = True
except ImportError:
    PIL_AVAILABLE = False


# ─────────────────────────────────────────────
# PALETA E FONTES
# ─────────────────────────────────────────────
BG_DARK      = "#0D0F14"
BG_CARD      = "#141720"
BG_CARD2     = "#1A1E2E"
ACCENT       = "#4F8EF7"
ACCENT2      = "#7C3AED"
TEXT_MAIN    = "#E8EAF0"
TEXT_MUTED   = "#6B7280"
TEXT_SUCCESS = "#34D399"
TEXT_ERROR   = "#F87171"
TEXT_WARN    = "#FBBF24"
BORDER       = "#252A3A"

FONT_TITLE = ("Courier New", 22, "bold")
FONT_LABEL = ("Courier New", 10, "bold")
FONT_SMALL = ("Courier New", 9)
FONT_MONO  = ("Courier New", 9)
FONT_BTN   = ("Courier New", 11, "bold")


# ─────────────────────────────────────────────
# FUNÇÕES DE API
# ─────────────────────────────────────────────
def upload_to_filestack(filepath: str, api_key: str) -> str:
    url = f"https://www.filestackapi.com/api/store/S3?key={api_key}"
    filename = os.path.basename(filepath)
    boundary = "----FilestackBoundary7x9z"

    with open(filepath, "rb") as f:
        file_content = f.read()

    content_type_map = {
        ".jpg": "image/jpeg", ".jpeg": "image/jpeg",
        ".png": "image/png",  ".gif": "image/gif",
        ".bmp": "image/bmp",  ".webp": "image/webp",
        ".tiff": "image/tiff", ".pdf": "application/pdf",
    }
    ext = os.path.splitext(filename)[1].lower()
    file_ct = content_type_map.get(ext, "application/octet-stream")

    body = (
        f"--{boundary}\r\n"
        f'Content-Disposition: form-data; name="fileUpload"; filename="{filename}"\r\n'
        f"Content-Type: {file_ct}\r\n\r\n"
    ).encode() + file_content + f"\r\n--{boundary}--\r\n".encode()

    req = urllib.request.Request(
        url, data=body,
        headers={
            "Content-Type": f"multipart/form-data; boundary={boundary}",
            "Content-Length": str(len(body)),
        },
        method="POST",
    )
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode()
    except urllib.error.HTTPError as e:
        body_err = e.read().decode(errors="replace")
        raise RuntimeError(f"Erro no upload (HTTP {e.code}): {body_err[:300]}")

    try:
        data = json.loads(raw)
    except json.JSONDecodeError:
        raise RuntimeError(f"Upload retornou resposta invalida:\n{raw[:300]}")

    handle = data.get("handle")
    if not handle:
        raise RuntimeError(f"Handle nao encontrado na resposta:\n{raw[:300]}")
    return handle


def run_ocr(handle: str, api_key: str, policy: str, signature: str) -> dict:
    policy    = (policy or "").strip()
    signature = (signature or "").strip()

    if policy and signature:
        url = (
            f"https://cdn.filestackcontent.com/"
            f"security=p:{policy},s:{signature}"
            f"/ocr/{handle}"
        )
    else:
        url = f"https://cdn.filestackcontent.com/{api_key}/ocr/{handle}"

    req = urllib.request.Request(url, method="GET")
    try:
        with urllib.request.urlopen(req) as resp:
            raw = resp.read().decode()
    except urllib.error.HTTPError as e:
        body_err = e.read().decode(errors="replace")
        if e.code == 400:
            raise RuntimeError(
                f"Erro 400 - Bad Request.\n"
                f"Verifique se Policy e Signature estao corretas.\n\n"
                f"Detalhe: {body_err[:400]}"
            )
        elif e.code in (401, 403):
            raise RuntimeError(
                f"Erro {e.code} - Nao autorizado.\n"
                f"API Key, Policy ou Signature invalidas ou sem permissao OCR.\n\n"
                f"Detalhe: {body_err[:400]}"
            )
        else:
            raise RuntimeError(f"Erro HTTP {e.code}:\n{body_err[:400]}")

    if not raw.strip():
        raise RuntimeError(
            "A API retornou resposta vazia.\n\n"
            "Possiveis causas:\n"
            "  - Security ativo na conta mas Policy/Signature em branco\n"
            "  - Policy ou Signature incorretas / expiradas\n"
            "  - OCR nao disponivel no seu plano Filestack"
        )

    try:
        return json.loads(raw)
    except json.JSONDecodeError:
        raise RuntimeError(f"Resposta nao e JSON valido:\n{raw[:500]}")


# ─────────────────────────────────────────────
# WIDGET: JSON colorizado
# ─────────────────────────────────────────────
class JSONViewer(tk.Text):
    def __init__(self, parent, **kwargs):
        super().__init__(
            parent, font=FONT_MONO, bg=BG_DARK, fg=TEXT_MAIN,
            insertbackground=ACCENT, selectbackground=ACCENT2,
            relief="flat", bd=0, wrap="none", **kwargs,
        )
        self.tag_configure("key",     foreground="#79B8FF")
        self.tag_configure("str",     foreground="#9ECBFF")
        self.tag_configure("num",     foreground="#F8C555")
        self.tag_configure("bool",    foreground="#F97583")
        self.tag_configure("null",    foreground="#B392F0")
        self.tag_configure("bracket", foreground="#E1E4E8")
        self.tag_configure("comma",   foreground=TEXT_MUTED)

    def set_json(self, data):
        self.config(state="normal")
        self.delete("1.0", "end")
        self._render(data, indent=0)
        self.config(state="disabled")

    def set_comment(self, text, color="key"):
        self.config(state="normal")
        self.delete("1.0", "end")
        self.insert("end", text, color)
        self.config(state="disabled")

    def _render(self, obj, indent=0):
        pad = "  " * indent
        if isinstance(obj, dict):
            self.insert("end", "{\n", "bracket")
            items = list(obj.items())
            for i, (k, v) in enumerate(items):
                self.insert("end", pad + "  ")
                self.insert("end", f'"{k}"', "key")
                self.insert("end", ": ", "bracket")
                self._render(v, indent + 1)
                if i < len(items) - 1:
                    self.insert("end", ",", "comma")
                self.insert("end", "\n")
            self.insert("end", pad + "}", "bracket")
        elif isinstance(obj, list):
            self.insert("end", "[\n", "bracket")
            for i, item in enumerate(obj):
                self.insert("end", pad + "  ")
                self._render(item, indent + 1)
                if i < len(obj) - 1:
                    self.insert("end", ",", "comma")
                self.insert("end", "\n")
            self.insert("end", pad + "]", "bracket")
        elif isinstance(obj, str):
            self.insert("end", f'"{obj}"', "str")
        elif isinstance(obj, bool):
            self.insert("end", str(obj).lower(), "bool")
        elif obj is None:
            self.insert("end", "null", "null")
        else:
            self.insert("end", str(obj), "num")


# ─────────────────────────────────────────────
# APLICAÇÃO PRINCIPAL
# ─────────────────────────────────────────────
class FilestackOCRApp(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title("Filestack OCR")
        self.configure(bg=BG_DARK)
        self.geometry("1160x780")
        self.minsize(960, 640)
        self.resizable(True, True)
        self.selected_file = None
        self.ocr_result    = None
        self._build_ui()
        self._center_window()

    def _center_window(self):
        self.update_idletasks()
        w, h = self.winfo_width(), self.winfo_height()
        x = (self.winfo_screenwidth()  // 2) - (w // 2)
        y = (self.winfo_screenheight() // 2) - (h // 2)
        self.geometry(f"{w}x{h}+{x}+{y}")

    def _build_ui(self):
        header = tk.Frame(self, bg=BG_DARK, pady=16)
        header.pack(fill="x", padx=30)
        tk.Label(header, text="FILESTACK OCR", font=FONT_TITLE,
                 bg=BG_DARK, fg=TEXT_MAIN).pack(side="left")
        tk.Label(header, text="Extracao de texto via imagem", font=FONT_SMALL,
                 bg=BG_DARK, fg=TEXT_MUTED).pack(side="left", padx=14, pady=6)
        tk.Frame(self, bg=BORDER, height=1).pack(fill="x", padx=30)

        main = tk.Frame(self, bg=BG_DARK)
        main.pack(fill="both", expand=True, padx=30, pady=16)
        main.columnconfigure(0, weight=1)
        main.columnconfigure(1, weight=2)
        main.rowconfigure(0, weight=1)

        self._build_left_panel(main)
        self._build_right_panel(main)

        self.status_var = tk.StringVar(value="Aguardando arquivo...")
        status_bar = tk.Frame(self, bg=BG_CARD, pady=7)
        status_bar.pack(fill="x")
        self.status_lbl = tk.Label(
            status_bar, textvariable=self.status_var,
            font=FONT_SMALL, bg=BG_CARD, fg=TEXT_MUTED, anchor="w",
        )
        self.status_lbl.pack(side="left", padx=20)

    def _build_left_panel(self, parent):
        frame = tk.Frame(parent, bg=BG_CARD)
        frame.grid(row=0, column=0, sticky="nsew", padx=(0, 10))
        frame.columnconfigure(0, weight=1)

        tk.Label(frame, text="IMAGEM", font=FONT_LABEL,
                 bg=BG_CARD, fg=ACCENT, anchor="w"
                 ).grid(row=0, column=0, sticky="ew", padx=18, pady=(16, 6))

        pw = tk.Frame(frame, bg=BG_CARD2,
                      highlightthickness=1, highlightbackground=BORDER)
        pw.grid(row=1, column=0, sticky="ew", padx=18)
        pw.columnconfigure(0, weight=1)
        self.preview_lbl = tk.Label(pw, text="Nenhuma imagem\nselecionada",
                                    font=FONT_SMALL, bg=BG_CARD2, fg=TEXT_MUTED,
                                    pady=40, justify="center")
        self.preview_lbl.grid(row=0, column=0, sticky="ew")

        self.file_var = tk.StringVar(value="")
        tk.Label(frame, textvariable=self.file_var, font=FONT_SMALL,
                 bg=BG_CARD, fg=TEXT_MUTED, wraplength=240, justify="left"
                 ).grid(row=2, column=0, sticky="ew", padx=18, pady=(6, 4))

        bf = tk.Frame(frame, bg=BG_CARD)
        bf.grid(row=3, column=0, sticky="ew", padx=18, pady=4)
        bf.columnconfigure(0, weight=1)
        self._make_btn(bf, "[ SELECIONAR IMAGEM ]", ACCENT,
                       self._select_file).grid(row=0, column=0, sticky="ew", pady=(0, 5))
        self.ocr_btn = self._make_btn(bf, "[ EXECUTAR OCR ]", ACCENT2,
                                      self._run_ocr_thread, state="disabled")
        self.ocr_btn.grid(row=1, column=0, sticky="ew")

        tk.Frame(frame, bg=BORDER, height=1).grid(
            row=4, column=0, sticky="ew", padx=18, pady=12)

        tk.Label(frame, text="CREDENCIAIS FILESTACK", font=FONT_LABEL,
                 bg=BG_CARD, fg=TEXT_MUTED, anchor="w"
                 ).grid(row=5, column=0, sticky="ew", padx=18, pady=(0, 6))

        creds = [
            ("API KEY  (obrigatorio)", "api_key_var"),
            ("POLICY   (se Security ativo)", "policy_var"),
            ("SIGNATURE (se Security ativo)", "signature_var"),
        ]
        for i, (lbl, attr) in enumerate(creds):
            tk.Label(frame, text=lbl, font=("Courier New", 8),
                     bg=BG_CARD, fg=TEXT_MUTED, anchor="w"
                     ).grid(row=6 + i*2, column=0, sticky="ew", padx=18, pady=(4, 0))
            var = tk.StringVar()
            setattr(self, attr, var)
            tk.Entry(frame, textvariable=var, font=FONT_SMALL,
                     bg=BG_CARD2, fg=TEXT_MAIN, insertbackground=ACCENT,
                     relief="flat", bd=6,
                     highlightthickness=1, highlightbackground=BORDER
                     ).grid(row=7 + i*2, column=0, sticky="ew", padx=18, pady=(0, 2))

        self.loading_var = tk.StringVar(value="")
        tk.Label(frame, textvariable=self.loading_var,
                 font=FONT_SMALL, bg=BG_CARD, fg=ACCENT
                 ).grid(row=13, column=0, pady=6)

    def _build_right_panel(self, parent):
        frame = tk.Frame(parent, bg=BG_CARD)
        frame.grid(row=0, column=1, sticky="nsew")
        frame.columnconfigure(0, weight=1)
        frame.rowconfigure(2, weight=1)

        hdr = tk.Frame(frame, bg=BG_CARD)
        hdr.grid(row=0, column=0, sticky="ew", padx=18, pady=(16, 6))
        hdr.columnconfigure(0, weight=1)
        tk.Label(hdr, text="RETORNO JSON", font=FONT_LABEL,
                 bg=BG_CARD, fg=ACCENT, anchor="w").grid(row=0, column=0, sticky="w")
        self.copy_btn = self._make_btn(hdr, "[ COPIAR ]", TEXT_MUTED,
                                       self._copy_json, small=True, state="disabled")
        self.copy_btn.grid(row=0, column=1)
        self.save_btn = self._make_btn(hdr, "[ SALVAR .JSON ]", TEXT_MUTED,
                                       self._save_json, small=True, state="disabled")
        self.save_btn.grid(row=0, column=2, padx=(4, 0))

        tk.Label(frame, text="TEXTO EXTRAIDO", font=FONT_LABEL,
                 bg=BG_CARD, fg=TEXT_MUTED, anchor="w"
                 ).grid(row=1, column=0, sticky="ew", padx=18, pady=(8, 2))
        self.extracted_text = tk.Text(
            frame, font=FONT_SMALL, bg=BG_CARD2, fg=TEXT_SUCCESS,
            relief="flat", bd=0, height=4, wrap="word",
            highlightthickness=1, highlightbackground=BORDER,
        )
        self.extracted_text.grid(row=1, column=0, sticky="ew", padx=18, pady=(0, 8))
        self.extracted_text.insert("end", "O texto detectado aparecera aqui...")
        self.extracted_text.config(state="disabled")

        jw = tk.Frame(frame, bg=BG_DARK,
                      highlightthickness=1, highlightbackground=BORDER)
        jw.grid(row=2, column=0, sticky="nsew", padx=18, pady=(0, 16))
        jw.columnconfigure(0, weight=1)
        jw.rowconfigure(0, weight=1)
        self.json_viewer = JSONViewer(jw)
        self.json_viewer.grid(row=0, column=0, sticky="nsew", padx=6, pady=6)
        ys = ttk.Scrollbar(jw, orient="vertical",   command=self.json_viewer.yview)
        xs = ttk.Scrollbar(jw, orient="horizontal", command=self.json_viewer.xview)
        self.json_viewer.configure(yscrollcommand=ys.set, xscrollcommand=xs.set)
        ys.grid(row=0, column=1, sticky="ns")
        xs.grid(row=1, column=0, sticky="ew")

        self.json_viewer.set_comment(
            "// Preencha as credenciais a esquerda,\n"
            "// selecione uma imagem e clique em\n"
            "// [ EXECUTAR OCR ] para ver o retorno aqui."
        )

    def _make_btn(self, parent, text, color, cmd, small=False, state="normal"):
        font = ("Courier New", 8, "bold") if small else FONT_BTN
        btn = tk.Button(
            parent, text=text, font=font, bg=BG_CARD2, fg=color,
            activebackground=BORDER, activeforeground=TEXT_MAIN,
            relief="flat", bd=0, pady=8 if not small else 4,
            cursor="hand2", command=cmd, state=state,
        )
        btn.bind("<Enter>", lambda e: btn.config(bg=BORDER))
        btn.bind("<Leave>", lambda e: btn.config(bg=BG_CARD2))
        return btn

    def _set_status(self, msg, color=TEXT_MUTED):
        self.status_var.set(msg)
        self.status_lbl.config(fg=color)

    def _select_file(self):
        path = filedialog.askopenfilename(
            title="Selecionar imagem",
            filetypes=[
                ("Imagens", "*.jpg *.jpeg *.png *.gif *.bmp *.webp *.tiff"),
                ("PDF", "*.pdf"), ("Todos", "*.*"),
            ],
        )
        if not path:
            return
        self.selected_file = path
        name = os.path.basename(path)
        size = os.path.getsize(path) / 1024
        self.file_var.set(f"{name}  |  {size:.1f} KB")
        self.ocr_btn.config(state="normal")
        self._set_status(f"Arquivo selecionado: {name}")

        if PIL_AVAILABLE:
            try:
                img = Image.open(path)
                img.thumbnail((280, 180))
                photo = ImageTk.PhotoImage(img)
                self.preview_lbl.config(image=photo, text="", pady=0)
                self.preview_lbl.image = photo
            except Exception:
                self.preview_lbl.config(image="",
                                        text=f"[{os.path.splitext(name)[1].upper()}]",
                                        pady=40)
        else:
            self.preview_lbl.config(
                text=f"[{os.path.splitext(name)[1].upper()}]\n{name}", pady=30)

    def _run_ocr_thread(self):
        if not self.selected_file:
            return
        api_key   = self.api_key_var.get().strip()
        policy    = self.policy_var.get().strip()
        signature = self.signature_var.get().strip()

        if not api_key:
            messagebox.showerror("Erro", "Preencha a API Key da Filestack!")
            return
        if bool(policy) != bool(signature):
            messagebox.showwarning(
                "Atencao",
                "Preencha AMBOS os campos Policy e Signature,\n"
                "ou deixe os DOIS em branco (se Security nao estiver ativo)."
            )
            return

        self.ocr_btn.config(state="disabled")
        self._animate_loading()
        threading.Thread(target=self._process_ocr,
                         args=(api_key, policy, signature), daemon=True).start()

    def _animate_loading(self, step=0):
        frames = ["o  Processando...", "O  Processando...", "0  Processando...", "o  Processando..."]
        self.loading_var.set(frames[step % len(frames)])
        self._loading_job = self.after(250, self._animate_loading, step + 1)

    def _stop_loading(self):
        if hasattr(self, "_loading_job"):
            self.after_cancel(self._loading_job)
        self.loading_var.set("")

    def _process_ocr(self, api_key, policy, signature):
        try:
            self.after(0, self._set_status, "Fazendo upload para Filestack...", ACCENT)
            handle = upload_to_filestack(self.selected_file, api_key)
            self.after(0, self._set_status, "Executando OCR...", ACCENT)
            result = run_ocr(handle, api_key, policy, signature)
            self.ocr_result = result
            self.after(0, self._show_result, result)
        except Exception as e:
            self.after(0, self._show_error, str(e))

    def _show_result(self, result):
        self._stop_loading()
        self.ocr_btn.config(state="normal")
        self.json_viewer.set_json(result)

        try:
            text = (result.get("data", {}) or {}).get("text") or result.get("text") or ""
        except Exception:
            text = ""

        self.extracted_text.config(state="normal")
        self.extracted_text.delete("1.0", "end")
        self.extracted_text.insert("end", text or "Nenhum texto detectado.")
        self.extracted_text.config(state="disabled")

        self.copy_btn.config(state="normal")
        self.save_btn.config(state="normal")
        self._set_status("OCR concluido com sucesso.", TEXT_SUCCESS)

    def _show_error(self, msg):
        self._stop_loading()
        self.ocr_btn.config(state="normal")
        self._set_status(f"Erro: {msg[:80]}", TEXT_ERROR)
        lines = "\n// ".join(msg.splitlines())
        self.json_viewer.set_comment(f"// ERRO\n// {lines}", "key")
        messagebox.showerror("Erro no OCR", msg)

    def _copy_json(self):
        if not self.ocr_result:
            return
        self.clipboard_clear()
        self.clipboard_append(json.dumps(self.ocr_result, indent=2, ensure_ascii=False))
        self._set_status("JSON copiado!", TEXT_SUCCESS)

    def _save_json(self):
        if not self.ocr_result:
            return
        path = filedialog.asksaveasfilename(
            defaultextension=".json",
            filetypes=[("JSON", "*.json"), ("Todos", "*.*")],
            initialfile="ocr_result.json",
        )
        if path:
            with open(path, "w", encoding="utf-8") as f:
                json.dump(self.ocr_result, f, indent=2, ensure_ascii=False)
            self._set_status(f"JSON salvo em: {path}", TEXT_SUCCESS)


if __name__ == "__main__":
    app = FilestackOCRApp()
    app.mainloop()