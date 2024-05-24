import re
import traceback
import requests
from bs4 import BeautifulSoup
from difflib import SequenceMatcher
import json

discord_message = ""

def fetch_events():
    print('FETCHING EVENTS FOR WUTHERING WAVES...')
    url = "https://wutheringwaves.gg/news/event/"
    response = requests.get(url)
    response.raise_for_status()
    soup = BeautifulSoup(response.text, 'html.parser')
    # with open("wuthering_waves_events.html", "w", encoding="utf-8") as file: file.write(response.text)
    # with open("wuthering_waves_events.html", "r", encoding="utf-8") as file: html_content = file.read()
    # soup = BeautifulSoup(html_content, 'html.parser')

    events = soup.select('.entry-card')
    # statuses = ['Current', 'Upcoming', 'Permanent']
    # all_events = {'Current': [], 'Upcoming': [], 'Permanent': []}
    all_events = []

    for i, event in enumerate(events):
        try:
            print(f'[{i}] getting event:', end=' ')
            name = event.select_one(".entry-title").text.strip()
            print(name)
            imageURL = event.select_one('img').get('src')
            duration = (event.select_one('.ct-meta-element-date').text, None)
            type = [event.select_one(".ct-term-12").text]
            status = None
            page = event.select_one(".entry-title > a").get('href')
            
            event_item = {
                'event': name, 
                'image': imageURL,
                'duration': duration,
                'type': type,
                'status': status,
                'page': page
            }

            all_events.append(event_item)
        except Exception as e:
            print("Error: " + str(e.args[0]))
            global discord_message
            error_count = discord_message.count('\n')+1
            discord_message += f"> `[{error_count}] Error: {str(e.args[0])}`\n"

    return all_events


def fetch_codes():
    print('FETCHING CODES FOR WUTHERING WAVES...')
    url = "https://game8.co/games/Wuthering-Waves/archives/453149"
    response = requests.get(url)
    response.raise_for_status()
    soup = BeautifulSoup(response.text, 'html.parser')
    # with open("wuthering_waves_codes.html", "w", encoding="utf-8") as file: file.write(response.text)
    # with open("wuthering_waves_codes.html", "r", encoding="utf-8") as file: html_content = file.read()
    # soup = BeautifulSoup(html_content, 'html.parser')

    table = soup.select('.a-table')[0].select('tr:not(:first-child)')
    codes = {'activeCodes': [], 'expiredCodes': []}
    for index, code_row in enumerate(table):
        try:
            print(f'[{index}] getting code:', end=' ')
            code = code_row.select('td')[0].text.strip()
            print(code)
            server = "All"
            rewards = parse_rewards(code_row.select('td')[1])
            duration = (None, None)     #parse_duration(code_row.select('td')[3])
            is_expired = None   #'Expired:' in code_row.select('td')[3].text

            code_item = {
                'code': code,
                'server': server,
                'rewards': rewards,
                'duration': duration,
                'isExpired': is_expired
            }

            if rewards == []:
                pass
            elif is_expired:
                codes['expiredCodes'].append(code_item)
            else:
                codes['activeCodes'].append(code_item)
        except Exception as e:
            print("Error: " + str(e.args[0]))
            global discord_message
            error_count = discord_message.count('\n')+1
            discord_message += f"> `[{error_count}] Error: {str(e.args[0])}`\n"

    return codes

def parse_rewards(html_element):
    with open("wuwa-items-list.htm", "r", encoding="utf-8") as file: item_list_content = file.read()
    item_list_soup = BeautifulSoup(item_list_content, 'html.parser')
    rewards_list = []

    for item in html_element.text.split('●')[1:]:
        it = re.match(r"(.+) x(\d+)$", item.strip())
        name = it.group(1)
        amount = it.group(2)
        
        rarity, image_url = None, None
        for row in item_list_soup.select('table.items-table tr'):
            row_name = row.select_one('div.name')
            if (row_name != None) and (SequenceMatcher(None, name, row_name.text).ratio() >= 0.8):
                rarity = re.match(r"quality(\d+)", row_name.get('class')[1]).group(1)
                image_url = "https://wuthering.gg" + row.select_one('img').get('src')
                break

        rewards_list.append({
            'name': name,
            'amount': amount,
            'rarity': rarity,
            'imageURL': image_url
        })
    return rewards_list


def parse_duration(duration_html):
    duration = str(duration_html).split('>')
    for i in range(len(duration)): 
        duration[i] = duration[i].replace('<br/', '').replace('</b', '').strip()
    return [duration[1], duration[3]]


def save_to_json(events, codes):
    data = {
        'Events': events,
        'Codes': codes
    }

    with open("data-scraper/wuthering-waves-data.json", "w") as json_file:
        json.dump(data, json_file, indent=4)

def discord_notify(content, error=False):
    discord_id = "1022735992014254183"
    discord_webhook = "https://ptb.discord.com/api/webhooks/1031955469998243962/UO379MCHeXTXwk9s86qeZedKKNOa5aDVMHInqGea_dUEOzfPZf66i00CPbGOA0lOkIxp"
    
    if error:
        content =   f"<@{discord_id}> {content}" \
                    f"Check <https://github.com/jeryjs/data-scraper-for-wuthering-waves-helper/actions/workflows/actions.yml> for more details."

    payload = {
        'username': 'WuWa Scraper',
        'avatar_url': 'https://cdn.openart.ai/uploads/image_jE7q2WyA_1692912347314_512.webp',
        'content': content,
    }
    
    headers = {
        "Content-Type": "application/json"
    }
    
    response = requests.post(discord_webhook, json=payload, headers=headers)
    
    if response.status_code == 204:
        print("Successfully notified via Discord.")
    else:
        print(f"Failed to notify via Discord. Status code: {response.status_code}")


def run_with_error_handling(func):
    try:
        return func()
    except Exception as e:
        print("Error: " + str(e.args[0]))
        traceback.print_exc()
        global discord_message
        error_count = discord_message.count('\n')+1
        discord_message += f"> `[{error_count}] Error: {str(e.args[0])}`\n"
        discord_notify(f"Whoops~ Looks like HoYo Scraper ran into the following errors:\n{discord_message}", True)

if __name__ == "__main__":
    events = run_with_error_handling(fetch_events)
    codes = run_with_error_handling(fetch_codes)
    save_to_json(events, codes)
    
    discord_notify(f"`wuthering-waves-data.json` was updated.\n{discord_message}")
