const BASE_URL =
  process.env.BACKEND_INTERNAL_URL ||
  process.env.NEXT_PUBLIC_BACKEND_URL ||
  "http://localhost:5001";

export interface ApiFetchOptions extends Omit<RequestInit, "body"> {
  body?: unknown;
}

export interface BackendErrorPayload {
  code?: string;
  message?: string;
}

export class ApiError extends Error {
  constructor(
    public readonly status: number,
    public readonly code: string,
    message: string
  ) {
    super(message);
    this.name = "ApiError";
  }
}

/**
 * Funzione client HTTP universale: effettua la richiesta al backend,
 * gestisce headers/body JSON e restituisce la risposta tipizzata.
 * In caso di errore HTTP o di rete, lancia un'eccezione ApiError.
 */
export async function apiFetch<T = unknown>(
  endpoint: string,
  options: ApiFetchOptions = {}
): Promise<T> {
  const { body, headers, ...restOptions } = options;

  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${endpoint}`, {
      headers: {
        ...(body !== undefined ? { "Content-Type": "application/json" } : {}),
        ...headers,
      },
      ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
      ...restOptions,
    });
  } catch (err) {
    console.error(`Network error on [${endpoint}]:`, err);
    throw new ApiError(0, "CONNECTION_ERROR", (err as Error).message || "Connection error");
  }

  if (!res.ok) {
    const errorPayload = (await res.json().catch(() => ({}))) as BackendErrorPayload;
    const code = errorPayload.code || "SERVER_ERROR";
    const message = errorPayload.message || `Request failed with status ${res.status}`;
    throw new ApiError(res.status, code, message);
  }

  if (res.status === 204) {
    return undefined as unknown as T;
  }

  return (await res.json().catch(() => ({}))) as T;
}

export interface SafeApiResponse<T = unknown> {
  data?: T;
  error?: string;
  status?: number;
}

/**
 * Wrapper Result-based per Server Actions o contesti che preferiscono
 * un oggetto `{ data, error, status }` senza lanciare eccezioni.
 */
export async function safeApiFetch<T = unknown>(
  endpoint: string,
  options: ApiFetchOptions = {}
): Promise<SafeApiResponse<T>> {
  try {
    const data = await apiFetch<T>(endpoint, options);
    return { data, status: 200 };
  } catch (err) {
    if (err instanceof ApiError) {
      return {
        error: err.code || "SERVER_ERROR",
        status: err.status,
      };
    }
    return {
      error: "CONNECTION_ERROR",
      status: 0,
    };
  }
}
