/// <reference types="vitest" />
/// <reference types="@testing-library/jest-dom" />

declare module 'vitest' {
  interface Assertion<T = any> extends jest.Matchers<unknown, T> {}
  interface AsymmetricMatchersContaining extends jest.Matchers<unknown, unknown> {}
}
