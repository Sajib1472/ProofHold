import { test, expect } from "@playwright/test";

test("staff logs item, Alice claims, staff approves, pickup booked", async ({ page }) => {
  await page.goto("/login");
  await page.getByLabel("Email").fill("staff@proofhold.local");
  await page.getByLabel("Password").fill("proofhold");
  await page.getByRole("button", { name: "Sign in" }).click();
  await expect(page.getByRole("heading", { name: "Staff desk" })).toBeVisible();

  await page.getByRole("link", { name: "Log a found item" }).click();
  await page.getByRole("button", { name: "Log item" }).click();
  await expect(page.getByRole("heading", { name: "Staff item" })).toBeVisible();
  const staffUrl = page.url();
  const itemId = staffUrl.split("/").pop();

  await page.getByRole("button", { name: /Log out/ }).click();
  await page.goto("/login");
  await page.getByLabel("Email").fill("alice@proofhold.local");
  await page.getByLabel("Password").fill("proofhold");
  await page.getByRole("button", { name: "Sign in" }).click();

  await page.goto(`/items/${itemId}`);
  await page.getByRole("link", { name: "Start a claim" }).click();
  await page.getByLabel("What initials are inside?").fill("JS");
  await page.getByLabel("About how many cards?").fill("8");
  await page.getByRole("button", { name: "Submit claim" }).click();
  await expect(page.getByText("Pending staff review")).toBeVisible();

  await page.getByRole("button", { name: /Log out/ }).click();
  await page.goto("/login");
  await page.getByLabel("Email").fill("staff@proofhold.local");
  await page.getByLabel("Password").fill("proofhold");
  await page.getByRole("button", { name: "Sign in" }).click();
  await page.goto(`/staff/items/${itemId}`);
  await page.getByRole("button", { name: "Approve" }).click();

  const start = new Date();
  start.setDate(start.getDate() + 1);
  start.setHours(10, 0, 0, 0);
  const end = new Date(start);
  end.setHours(11, 0, 0, 0);
  const toLocal = (d: Date) => {
    const pad = (n: number) => String(n).padStart(2, "0");
    return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
  };
  await page.locator('input[type="datetime-local"]').first().fill(toLocal(start));
  await page.locator('input[type="datetime-local"]').nth(1).fill(toLocal(end));
  await page.getByRole("button", { name: "Book pickup" }).click();
  await expect(page.getByText("READY_FOR_PICKUP")).toBeVisible();
});
