export const pieData = [
  { name: "Concluídos", value: 82 },
  { name: "Pendentes", value: 18 },
];

export const PIE_COLORS = ["#F5C518", "#5A6B7B"];

export const lineData = [
  { mes: "Janeiro", valor: 8000 },
  { mes: "Fevereiro", valor: 14000 },
  { mes: "Março", valor: 9500 },
  { mes: "Abril", valor: 17000 },
  { mes: "Maio", valor: 21500 },
  { mes: "Junho", valor: 25000 },
  { mes: "Julho", valor: 22000 },
];

export const barData = [
  { categoria: "Pintura", valor: 28000 },
  { categoria: "Alvenaria", valor: 22000 },
  { categoria: "Demolição", valor: 42000 },
  { categoria: "Equipamentos", valor: 9000 },
  { categoria: "Mão de Obra", valor: 27000 },
  { categoria: "Custos extras", valor: 14000 },
];

export const formatBRL = (v: number) =>
  `R$${v.toLocaleString("pt-BR", { minimumFractionDigits: 2 })}`;
