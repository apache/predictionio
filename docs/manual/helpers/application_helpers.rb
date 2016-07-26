module ApplicationHelpers
  def page_title
    if current_page.data.title
      content_tag :h1 do
        rendered_title
      end
    else
      content_tag :h1, class: 'missing' do
        'Missing Title'
      end
    end
  end

  def rendered_title
    return unless current_page.data.title
    title = current_page.data.title
    template = Tilt['erb'].new { title }
    template.render(self, current_page.data)
  end

  def github_url
    base = 'https://github.com/apache/incubator-predictionio/tree/livedoc/docs/manual'
    path = current_page.source_file.sub(Middleman::Application.root_path.to_s, '')
    base + path
  end

  def page_title_in_nav_menu(nodes)
    def is_current_page(node)
      if node.url == current_page.url
        return true
      else
        return false
      end
    end

    if nodes
      result = ""
      nodes.each do |node|
        if node.children
          node.children.each do |child|
            if is_current_page(child)
              result = child
            end
          end
        else
          if is_current_page(node)
            result = node
          end
        end
      end
      if result != ""
        return result.body
      else
        return current_page.data.title
      end
    else
      return "Welcome to Apache PredictionIO (incubating) Documentation!"
    end

  end

  def link_to_with_active(body, url, options = {})
    if url == current_page.url
      link_to body, url, options.merge(class: [options[:class], 'active'].join(' '))
    else
      link_to body, url, options
    end
  end
end
