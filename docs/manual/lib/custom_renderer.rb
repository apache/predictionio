require 'middleman-core/renderers/redcarpet'

class CustomRenderer < Middleman::Renderers::MiddlemanRedcarpetHTML
  def initialize(options = {})
    defaults = { with_toc_data: true }
    super(defaults.merge(options))
  end

  def paragraph(text)
    case text
    when/\A(INFO|SUCCESS|WARNING|DANGER|NOTE|TODO):/
      convert_alerts(text)
    else
      %Q(<p>#{text}</p>)
    end
  end

  def block_html(raw_html)
    doc = Nokogiri::HTML::DocumentFragment.parse(raw_html)
    nodes = doc.css('div.tabs > div')

    if nodes.empty?
      raw_html
    else
      ul = Nokogiri::XML::Node.new('ul', doc)

      nodes.each do |node|
        title = node.attribute('data-tab').to_s
        uuid = SecureRandom.uuid
        id = "tab-#{uuid}"

        li = Nokogiri::XML::Node.new('li', doc)
        li.inner_html = %Q(<a href="##{id}">#{title}</a>)

        ul.add_child(li)

        markdown = Redcarpet::Markdown.new(CustomRenderer, fenced_code_blocks: true)
        output = markdown.render(node.inner_html)

        node.inner_html = output
        node['id'] = id
      end

      nodes.first.before(ul)

      doc.to_html
    end
  end

  private

  def convert_alerts(text)
    text.gsub(/\A(INFO|SUCCESS|WARNING|DANGER|NOTE|TODO):(.*?)(\n(?=\n)|\z)/m) do
      css_class = $1.downcase
      content = $2.strip
      %Q(<div class="alert #{css_class}"><p>#{content}</p></div>)
    end
  end
end