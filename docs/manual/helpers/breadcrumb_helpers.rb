require 'rainbow/ext/string'

module BreadcrumbHelpers

  def breadcrumbs
    result = false
    data.nav.main.root.each do |node|
      result = breadcrumb_search(current_page.url, node)
      break if result
    end

    partial 'nav/breadcrumbs', locals: { crumbs: result }

  end


  def breadcrumb_search(path, node, depth = 0, crumb = [])
    crumb[depth] = node
    return crumb if node.url == path

    if node.children
      node.children.each do |child|
        result = breadcrumb_search(path, child, depth + 1, crumb)
        return result if result
      end
    end

    false
  end
end
