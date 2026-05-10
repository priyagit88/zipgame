from playwright.sync_api import sync_playwright

def verify_walls_and_lines():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        # Navigate to the game
        page.goto("http://localhost:8080")
        
        # Wait for the grid to load
        page.wait_for_selector(".grid-container")
        
        # Reset game to ensure clean slate
        page.evaluate("fetch('/api/restart', {method: 'POST'})")
        page.reload()
        page.wait_for_selector(".grid-container")

        # Make a move that creates a line (1 is at 0,0)
        # 2 should be at (0,1).
        # We need to click (0,1).
        
        cells = page.locator(".cell")
        
        # Click (0,1) -> 2
        cells.nth(1).click()
        page.wait_for_timeout(200)

        # Click (0,2) -> 3
        cells.nth(2).click()
        page.wait_for_timeout(200)
        
        # Click (0,3) -> 4
        cells.nth(3).click()
        page.wait_for_timeout(200)

        # Take screenshot to verify lines and walls
        # The wall should be visible below (0,0) [index 0]
        # Lines should connect 1-2-3-4
        page.screenshot(path="verification/walls_and_lines.png")
        
        browser.close()

if __name__ == "__main__":
    verify_walls_and_lines()
