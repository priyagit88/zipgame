from playwright.sync_api import sync_playwright

def verify_random_level():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        # Navigate to the game
        page.goto("http://localhost:8080")
        
        # Wait for the grid to load
        page.wait_for_selector(".grid-container")
        
        # Restart to get a fresh random level
        page.get_by_text("Restart Level").click()
        page.wait_for_timeout(500)
        
        # Take a screenshot of the random level
        page.screenshot(path="verification/random_level_1.png")
        
        # Restart again to verify randomness
        page.get_by_text("Restart Level").click()
        page.wait_for_timeout(500)
        
        page.screenshot(path="verification/random_level_2.png")
        
        browser.close()

if __name__ == "__main__":
    verify_random_level()
