from playwright.sync_api import sync_playwright

def verify_game_state():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        
        # Navigate to the game
        page.goto("http://localhost:8080")
        
        # Wait for the grid to load
        page.wait_for_selector(".grid-container")
        
        # Check initial state
        # The first number (1) should be fixed and highlighted/identifiable
        # We look for a cell with text "1"
        cell_1 = page.locator(".cell").filter(has_text="1")
        if cell_1.count() == 0:
            print("Failed to find cell with 1")
            browser.close()
            return
            
        print("Found cell with 1")

        # Take initial screenshot
        page.screenshot(path="verification/initial_state.png")
        
        # Click a valid neighbor (0,1) which should be empty initially.
        # (0,0) is 1. (0,1) is to the right.
        # The grid is 6x6. 
        # The cells are generated in order. row 0 has indices 0-5.
        # We can find the cell at row 0, col 1.
        # In the DOM order, it's the 2nd child of grid-container (index 1)
        
        cells = page.locator(".cell")
        cell_target = cells.nth(1) # Index 1 is (0,1)
        cell_target.click()
        
        # Wait for update
        page.wait_for_timeout(500)
        
        # Check if it now contains '2'
        if "2" in cell_target.inner_text():
            print("Move successful, cell contains 2")
        else:
            print("Move failed")
            
        # Take screenshot after move
        page.screenshot(path="verification/after_move.png")
        
        browser.close()

if __name__ == "__main__":
    verify_game_state()
