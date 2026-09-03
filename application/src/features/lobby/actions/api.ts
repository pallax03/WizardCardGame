"use server";

import { safeApiFetch, type ApiFetchOptions } from "@/lib/api/api";
import type { ApiResponse } from "../types";

export async function apiFetch<T = unknown>(
  endpoint: string,
  options: ApiFetchOptions = {}
): Promise<ApiResponse<T>> {
  return safeApiFetch<T>(endpoint, options);
}