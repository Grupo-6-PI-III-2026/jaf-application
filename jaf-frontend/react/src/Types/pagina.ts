// Mesmo formato do PageResponse do backend.
export interface Pagina<T> {
  content: T[];
  number: number;
  size: number;
  totalElements: number;
  totalPages: number;
}