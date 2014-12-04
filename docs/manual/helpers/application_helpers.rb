module ApplicationHelpers
  def page_title
    if current_page.data.title
      content_tag :h1 do
        current_page.data.title
      end
    else
      content_tag :h1, class: 'missing' do
        'Missing Title'
      end
    end
  end

  def github_url
    base = 'https://github.com/PredictionIO/PredictionIO/tree/livedoc/docs/manual'
    path = current_page.source_file.sub(Middleman::Application.root_path.to_s, '')
    base + path
  end

  def link_to_with_active(body, url)
    if url == current_page.url
      link_to body, url, class: 'active'
    else
      link_to body, url
    end
  end
end