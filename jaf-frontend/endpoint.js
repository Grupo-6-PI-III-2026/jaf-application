// Função para mostrar mensagens no frontend
function mostrarMensagem(mensagem, tipo = 'info') {
    const containerMensagens = document.getElementById('mensagens');
    const divMensagem = document.createElement('div');
    divMensagem.className = `mensagem ${tipo}`;
    
    if (tipo === 'dados' && typeof mensagem === 'object') {
        divMensagem.textContent = JSON.stringify(mensagem, null, 2);
    } else {
        divMensagem.textContent = mensagem;
    }
    
    containerMensagens.appendChild(divMensagem);
    
    divMensagem.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
    
    setTimeout(() => {
        divMensagem.style.opacity = '0';
        setTimeout(() => divMensagem.remove(), 300);
    }, 3000);
}

function validarToken() {
    const token = sessionStorage.getItem('token');
    if (!token) return null;
    return (
        JSON.parse(atob(token))
    );
}

// BUSCAR USUARIOS
async function buscar() {
    const dado = validarToken();

    if (!dado) {
        mostrarMensagem('Nenhum usuario logado', 'erro');
        return;
    }

    if (dado.role.toLowerCase() === "admin") {
        const resposta = await fetch('http://localhost:3000/usuarios');
        const dados = await resposta.json();
        mostrarMensagem('Usuários encontrados:', 'sucesso');
        mostrarMensagem(dados, 'dados');
    } else {
        mostrarMensagem('Acesso negado, Você nao tem permissão de acesso', 'erro');
    }
}

// LOGIN
async function login() {
    let email = input_login_email.value.toLowerCase().trim();
    let senha = input_login_senha.value;

    if (email.trim() === "" || senha.trim() === "") {
        mostrarMensagem('Por favor, preencha o e-mail e a senha.', 'erro');
        return;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailRegex.test(email)) {
        mostrarMensagem('Formato de e-mail inválido.', 'erro');
        return;
    }

    const resposta = await fetch(`http://localhost:3000/usuarios?email=${email}`);
    const usuariosEncontrado = await resposta.json(); 

    if (usuariosEncontrado.length > 0) {
        let usuario = usuariosEncontrado[0];
        
        // Validar senha
        if (usuario.senha === senha) {
            const payload = JSON.stringify({ id: usuario.id, role: usuario.role });
            const token = btoa(payload);

            sessionStorage.setItem('token', token);
            mostrarMensagem(`Login realizado com sucesso para ${usuario.nome || usuario.email}`, 'sucesso');
            carregarObras();
        } else {
            sessionStorage.removeItem('token');
            mostrarMensagem('Usuário ou senha incorretos', 'erro');
        }
    } else {
        sessionStorage.removeItem('token');
        mostrarMensagem('Usuário ou senha incorretos', 'erro');
    }
}

// VER OBRAS
async function obras() {
    const dado = validarToken();

    if (!dado) {
        mostrarMensagem('Nenhum usuario logado', 'erro');
        return;
    }
    
    const rolesPermitidas = ["admin", "gestor_obra", "operador_lancamento"];
    if (rolesPermitidas.includes(dado.role.toLowerCase())) {
        const resposta = await fetch('http://localhost:3000/obras');
        const dados = await resposta.json();
        mostrarMensagem('Obras encontradas:', 'sucesso');
        mostrarMensagem(dados, 'dados');
    } else {
        mostrarMensagem('Você não tem permissão de acesso', 'erro');
    }
}

// Obra Expecifica
async function obra() {
    const idObra = input_obra.value.trim();
    const dado = validarToken();

    if (!dado) {
        mostrarMensagem('Nenhum usuario logado', 'erro');
        return;
    }

    if (!idObra) {
        mostrarMensagem('Informe uma obra', 'erro');
        return;
    }

    const rolesPermitidas = ["admin", "gestor_obra", "operador_lancamento"];
    if (rolesPermitidas.includes(dado.role.toLowerCase())) {
        const resposta = await fetch(`http://localhost:3000/obras/${idObra}`);
        
        if (resposta.ok) {
            const dados = await resposta.json();
            mostrarMensagem('Obra encontrada:', 'sucesso');
            mostrarMensagem(dados, 'dados');
        } else {
            mostrarMensagem('Obra não encontrada', 'erro');
        }
    } else {
        mostrarMensagem('Acesso negado, Você não tem permissão de acesso', 'erro');
    }
}

// Criar usuario (precisa ser adm)
async function CriarUser() {
    const dado = validarToken();

    if (!dado) {
        mostrarMensagem('Nenhum usuario logado', 'erro');
        return;
    }

    if (dado.role.toLowerCase() === "admin") {
        const nome  = input_cadastro_nome.value.trim();
        const email = input_cadastro_email.value.toLowerCase().trim();
        const senha = input_cadastro_senha.value;
        const role = input_cadastro_role.value;

        if (!nome) {
            mostrarMensagem('Nome é obrigatório para cadastro', 'erro');
            return;
        }

        if (email.trim() === "" || senha.trim() === "") {
            mostrarMensagem('Email e senha são obrigatórios para cadastro', 'erro');
            return;
        }

        if (!role) {
            mostrarMensagem('Selecione um perfil para o usuário', 'erro');
            return;
        }

        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(email)) {
            mostrarMensagem('Insira um e-mail válido para cadastrar', 'erro');
            return;
        }

        if (senha.length < 6) {
            mostrarMensagem('A senha deve ter pelo menos 6 caracteres', 'erro');
            return;
        }

        const verificarEmail = await fetch(`http://localhost:3000/usuarios?email=${email}`);
        const emailExistente = await verificarEmail.json();

        if (emailExistente.length > 0) {
            mostrarMensagem('Email já cadastrado', 'erro');
            return;
        }

        const resposta = await fetch('http://localhost:3000/usuarios', {
            method: "POST",
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                nome,
                email,
                senha,
                role
            })
        });

        const dados = await resposta.json();
        mostrarMensagem(`Usuário ${nome} cadastrado com sucesso!`, 'sucesso');
        mostrarMensagem(dados, 'dados');
        
        // Limpar campos
        input_cadastro_nome.value = '';
        input_cadastro_email.value = '';
        input_cadastro_senha.value = '';
        input_cadastro_role.value = '';
    } else {
        mostrarMensagem('Acesso negado, Você não tem permissão de acesso', 'erro');
    }
}

// Colocar os dados no select
async function carregarObras() {
    const dado = validarToken();
    if (!dado) {
        return;
    }

    const resposta = await fetch('http://localhost:3000/obras');
    const obras = await resposta.json();

    const select = input_gasto_obra;
    select.innerHTML = '<option value="">Selecione a obra</option>';

    obras.forEach(obra => {
        const option = document.createElement('option');
        option.value = obra.id;
        option.textContent = obra.titulo;
        select.appendChild(option);
    });
}

// Criar os gastos de acordo com a obra
async function registrarGasto() {
    const dado = validarToken();

    if (!dado) {
        mostrarMensagem('Nenhum usuario logado', 'erro');
        return;
    }

    const obra_id  = input_gasto_obra.value;
    const categoria = input_gasto_categoria.value;
    const pagamento = input_gasto_pagamento.value;
    const valor  = input_gasto_valor.value;
    const data = input_gasto_data.value;
    const descricao  = input_gasto_descricao.value.trim();

    if (!obra_id) {
        mostrarMensagem('Selecione uma obra', 'erro');
        return;
    }

    if (!categoria) {
        mostrarMensagem('Selecione uma categoria', 'erro');
        return;
    }

    if (!pagamento) {
        mostrarMensagem('Selecione uma forma de pagamento', 'erro');
        return;
    }

    if (!valor || parseFloat(valor) <= 0) {
        mostrarMensagem('Informe um valor válido', 'erro');
        return;
    }

    if (!data) {
        mostrarMensagem('Informe a data do gasto', 'erro');
        return;
    }

    const resposta = await fetch('http://localhost:3000/gastos', {
        method: "POST",
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            obra_id: parseInt(obra_id),
            categoria,
            pagamento,
            valor: parseFloat(valor),
            data,
            descricao,
            usuario_id: dado.id
        })
    });

    const dados = await resposta.json();
    mostrarMensagem('Gasto registrado com sucesso!', 'sucesso');
    mostrarMensagem(dados, 'dados');
    
    // Limpar campos
    input_gasto_categoria.value = '';
    input_gasto_pagamento.value = '';
    input_gasto_valor.value = '';
    input_gasto_data.value = '';
    input_gasto_descricao.value = '';
}

// Carregar obras ao iniciar a página se o usuário já estiver logado
window.addEventListener('DOMContentLoaded', () => {
    const dado = validarToken();
    if (dado) {
        carregarObras();
    }
});
