module IconHelpers
  def icon(name)
    if name.nil?
      %Q{<i class="fa"></i>}
    else
      %Q{<i class="fa fa-#{name}"></i>}
    end
  end
end
